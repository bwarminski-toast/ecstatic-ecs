## Goals

The goal of this tutorial is to launch a containerized application on a single AWS instance. 

## Prerequisites

- Docker installed (Docker for Mac on OSX)

```
$ docker -v
Docker version 18.09.2, build 6247962
```

- A webapp you'd like to deploy, built locally into a container image. (See 00-getting-started.md)

- The AWS CLI installed locally

```
$ aws help
```

If you don't have it installed, on Mac OSX do:

```
brew install awscli
```

- A public/private SSH keypair

```
$ ls ~/.ssh
authorized_keys	id_rsa		id_rsa.pub	known_hosts
```
(id_rsa and id_rsa.pub are present -- or you know where you put the other ones)

If you haven't yet generated one:

```
ssh-keygen -t rsa -b 4096
```

- jq

`jq` is a handy utility to parsing JSON on the command line. It makes automation of some aws cli commands a little more manageable.

```
$ jq -V
jq-1.6
```

If the command is missing, do:

```
brew install jq
```

- An AWS access key and secret stored in a safe place

Typically this is given to you by an administrator or is granted through special tools like SAML via Okta. To keep our examples
simple we'll assume a long lived access key and secret has been generated. 

A handy tool for storing long-lived AWS keys is `aws-vault`. `aws-vault` stores your access keys on the OSX keychain and has a simple
invocation syntax. To install it:

```
brew install aws-vault
```

You can add an access key and secret to the keychain using:

```
$ aws-vault add home
Enter Access Key Id: ABDCDEFDASDASF
Enter Secret Key: %

```

Subsequently, if you'd like to run an aws CLI or Terraform command with temporary credentials derived from your keychain, you can
simply run:

```
aws-vault exec home --assume-role-ttl=60m --

```

This will open a bash subshell with a temporary set of credentials stored in environment variables that are good for 60 minutes.

All terraform and aws commands in this tutorial assume you've set up credentials elsewhere.

## Steps

### 1. Connect to AWS and verify your identity

This is a quick step to validate that you AWS access keys are working and that AWS CLI is installed.

```
aws sts get-caller-identity
```

The output should be something like:
```
{
    "Account": "1111111", 
    "UserId": "ABCD", 
    "Arn": "arn:aws:iam::1111:user/bwarminski"
}
```

If you don't get this output or some type of error, your credentails are probably not set up correctly. Repeat some of the
prerequisite steps or look at the AWS CLI documentation online for guidance.

### 2. Push your container image to Amazon ECR

In order to host our containerized application on AWS, we need to push our Docker image to a remote Docker repository that
can be reached from the new instance. Amazon Elastic Container Registry (ECR) is a service similar to Docker Hub that provides 
this functionality.

To create a repository:

(Replace the repository-name value with a unique name for your project with out without a / )
```
aws ecr create-repository --repository-name webapps/bwarminski-hello --region us-east-1
```

The output should look something similar to this:
```
{
    "repository": {
        "registryId": "1111", 
        "repositoryName": "webapps/bwarminski-hello", 
        "repositoryArn": "arn:aws:ecr:us-east-1:1111:repository/webapps/bwarminski-hello", 
        "createdAt": 1561522311.0, 
        "repositoryUri": "1111.dkr.ecr.us-east-1.amazonaws.com/webapps/bwarminski-hello"
    }
}
```

The value of `repositoryUri` tells us where to push our docker image.

Our local Docker CLI will need to fetch a token to be able to authenticate against this repository. This can be accomplished
with:

```
$(aws ecr get-login --region us-east-1 --no-include-email)
``` 

This command will immediately execute the value returned from ecr get-login which is a templated `docker login` command that
looks something like:

```
docker login -u AWS -p someverrrrylongtoken1234def https://111.dkr.ecr.us-east-1.amazonaws.com
```

Now that our local Docker environment has authentiated with AWS, we can tag and push our image to the new repository:


(Replace REPOSITORY_URI with the `repositoryUri` from the `create-repositroy` output)
```
docker tag hello-webapp:v1.0 REPOSITORY_URI:v1.0
docker push REPOSITORY_URI:v.10
```

Once that is successful, we can move on to some other configuration pieces.

### 3. Import an SSH keypair

We need to take some steps to make sure that once we've provisioned our brand new EC2 instance, we have the ability
to access it remotely. 

First, we need to import our publish SSH key into AWS so that it can be present on the instance when it starts up.

(Replace my-key with a unique name before running)
```
aws ec2 import-key-pair --key-name "my-key" --public-key-material file://~/.ssh/id_rsa.pub --region us-east-1

```

Next, we need to define a security group for our instance.

### 4. Create a security group

Security groups are sets of firewall configuration policy that can be applied to an EC2 instance. They define rules for which
IP address ranges are allowed to access with inbound ports on the instance, and which external IP addresses are reachable from
within the instance.

Instances can have one more or security groups attached to them. However, once an instance is started, its security groups can not
change, which is why we're making one now.

NOTE: We're going to set up some pretty lax security group settings to keep the demonstration simple and allow the entire Internet
to reach the open SSH ports on our instance as well as its web addresses. This is a bad idea for any realistic workload. Bad guys
know how to scan the EC2 IP address range to find misconfigured hosts. Find a friend who cares deeply about security group settings
and a cup of coffee to get some advice for a more real world scenario.

To do this, we'll first create an "empty" security group, then we'll add some rules to that group.

To create a security group:

(Replace my-sg with a unique name for the group and description with something fun that you want to see at 3AM when on pager duty)
```
aws ec2 create-security-group --group-name my-sg --description "My security group" --region us-east-1
```

The output should look something like this:

```
{
    "GroupId": "sg-0d1550949cbb88b45"
}
```

Make note of the GroupId from here because we'll be using that value for subsequent commands.

Let's add 3 rules:
- Allow SSH access from anywhere on the internet to port 22
- Allow web access from anywhere on the internet to port 8080
- Allow admin access from anywhere on the internet to port 8081

By default, security groups will allow all outbound traffic, so it is not necessary to set a rule for that now.

(Replace GROUP_ID with the id of the security group you just created)
```
aws ec2 authorize-security-group-ingress --group-id GROUP_ID --protocol tcp --port 22 --cidr 0.0.0.0/0 --region us-east-1
aws ec2 authorize-security-group-ingress --group-id GROUP_ID --protocol tcp --port 8080 --cidr 0.0.0.0/0 --region us-east-1
aws ec2 authorize-security-group-ingress --group-id GROUP_ID --protocol tcp --port 8081 --cidr 0.0.0.0/0 --region us-east-1
```
  
Once we've done that, let's verify our settings. They should look something like this:

```
 aws ec2 describe-security-groups --group-ids sg-0d1550949cbb88b45 --region us-east-1
{
    "SecurityGroups": [
        {
            "IpPermissionsEgress": [
                {
                    "IpProtocol": "-1", 
                    "PrefixListIds": [], 
                    "IpRanges": [
                        {
                            "CidrIp": "0.0.0.0/0"
                        }
                    ], 
                    "UserIdGroupPairs": [], 
                    "Ipv6Ranges": []
                }
            ], 
            "Description": "Bretts first Security Group", 
            "IpPermissions": [
                {
                    "PrefixListIds": [], 
                    "FromPort": 8080, 
                    "IpRanges": [
                        {
                            "CidrIp": "0.0.0.0/0"
                        }
                    ], 
                    "ToPort": 8080, 
                    "IpProtocol": "tcp", 
                    "UserIdGroupPairs": [], 
                    "Ipv6Ranges": []
                }, 
                {
                    "PrefixListIds": [], 
                    "FromPort": 22, 
                    "IpRanges": [
                        {
                            "CidrIp": "0.0.0.0/0"
                        }
                    ], 
                    "ToPort": 22, 
                    "IpProtocol": "tcp", 
                    "UserIdGroupPairs": [], 
                    "Ipv6Ranges": []
                }, 
                {
                    "PrefixListIds": [], 
                    "FromPort": 8081, 
                    "IpRanges": [
                        {
                            "CidrIp": "0.0.0.0/0"
                        }
                    ], 
                    "ToPort": 8081, 
                    "IpProtocol": "tcp", 
                    "UserIdGroupPairs": [], 
                    "Ipv6Ranges": []
                }
            ], 
            "GroupName": "brett-sh-1", 
            "VpcId": "vpc-3faac945", 
            "OwnerId": "645643289692", 
            "GroupId": "sg-0d1550949cbb88b45"
        }
    ]
}
```

### 5. Launch our first instance

Now that we have our container image hosted on AWS and some basic remote access controls in place, let's start our first instance.

(Replace KEY_NAME with the SSH keyname you created in step 3 and SECURITY_GROUP with the id of the group created in step 4)
```
aws ec2 run-instances --image-id ami-02507631a9f7bc956 --count 1 --instance-type t2.medium --key-name KEY_NAME --security-group-ids SECURITY_GROUP --region us-east-1 | tee mynewinstance.json
```

The `image-id` argument refers to an Amazon Machine Image (AMI) which is a virtual machine image. The specific image in the example
above is an ECS/Docker optimized image that has Docker installed and configured to start at instance boottime. This will come in handy
later.

The `instance-type` argument refers to a specific instance configuration. There are tons of different EC2 instance types. The t2 family
tends to be cheap and useful for little bursty projects.

This command will output a whole bunch of information about the instance that was started. We've saved it off to a json file named 
`mynewinstance.json` for later inspection. The value we really need from the file is the instance ID, which we can grab with:

```
jq '.Instances[0].InstanceId' mynewinstance.json
```

Take a note of this value because it will be useful in later sections.

### 6. SSH onto the instance and start our webapp

In order to connect to this instance remotely, we'll need to obtain its internet facing IP address, which is assigned after the
instance has been created and is in the process of booting.

We can obtain it by making an API call to describe the instance:

(Replace INSTANCE_ID with the instance ID from step 5)
```
aws ec2 describe-instances --instance-ids INSTANCE_ID --region us-east-1 | jq '.Reservations[0].Instances[0].PublicIpAddress'
``` 

Thanks to our keypair and security group settings, we should be able to SSH to this instance using our own SSH key as the `ec2-user`
on the host.

```
$ ssh ec2-user@54.234.223.163
The authenticity of host '54.234.223.163 (54.234.223.163)' can't be established.
ECDSA key fingerprint is SHA256:0p04QrWXmqDwomFHg+yHivJljtEC1agp0hjpE7vS8r8.
Are you sure you want to continue connecting (yes/no)? yes
Warning: Permanently added '54.234.223.163' (ECDSA) to the list of known hosts.

   __|  __|  __|
   _|  (   \__ \   Amazon Linux 2 (ECS Optimized)
 ____|\___|____/

For documentation, visit http://aws.amazon.com/documentation/ecs
[ec2-user@ip-172-31-80-24 ~]$ 
```

We can test that Docker is running on the host with a `docker ps`

```
$ docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES

```

Let's authenticate our host with docker and run our image on the host:

In a separate window on your local host, run:

```
aws ecr get-login --region us-east-1 --no-include-email
```

and paste and run the generate command in the SSH terminal, then:

(Replace REPOSITORY_URI from with URI from step 2)
```
docker run -p 8080:8080 -it REPOSITORY_URI:v1.0
```

Back in the other terminal, curl port 8080 on this instance's public ip address and the app should be running!

### 7. Clean Up

You should be very pleased with yourself. With some cut and paste and a few CLI runs, you've done something that took a specialized team
and a week turnaround time a few years ago. It was still painful to do all the steps by hand, but it's something we can automate in the
next tutorial.

For now, let's terminate the instance:

```
aws ec2 terminate-instances --instance-ids YOUR_INSTANCE_ID --region us-east-1 
```

## Congratulations!

You've successfully deployed your first containerized app to AWS! In the next section, we'll introduce some new tools that will
help us automate the provision-startup steps, and make our new web app more scalable and resilient to outages.
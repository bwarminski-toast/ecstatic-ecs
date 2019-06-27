## Goals

The goal of this tutorial is to automate the manual steps of our containerized deploy and to make it more scalable and resilient.

## Prerequisites

- Completion of 01-basic-ec2 OR: Docker, AWS CLI, AWS credentials, a docker repository, keypair and basic security group setup

- Terraform

```
$ which terraform
/usr/local/bin/terraform
```

This isn't strictly necessary because all actions can be performed using the AWS CLI or the console, but Terraform comes in handy
when there are a lot of configuration variables that need to get passed to create different AWS resources.

If missing:

```
brew install terraform
```


## Steps

### 1. Give your instance ECR Permissions

Typically this is something that will be done by an administrator, but it's important to know the mechanism that is involved.

At the end of our last tutorial, we ended up opening a local terminal window and generating docker credentials for our instance
to pull the image from ECR. This was less than ideal. Even in a situation where we could have the instance pull from docker on its
own, we would still have needed to copy over some form of long-lasting credentials for the instance itself to use to authenticate
with AWS and stored them as a secret on the host. Eventually those credentials would get copy-pasted by someone who needed them at the time,
and a few months later everyone has the keys, and then the world would end.

What we'd really like is for the instance itself to be granted enough privileges with AWS to log into ECR on its own
accord. EC2 instances have a setting called the Instance Profile, that allows for this type of behavior. An instance profile is 
simply a collection of policy statements that declare which AWS resources an EC2 instance is allowed to access. Once the instance
is running, AWS provides an HTTP endpoint that supplies temporary credentials specific to the host  that can be
used to access the resources defined in the policy.

Amazon's IAM model is too complex to bring into the scope of this tutorial for now. So, in order to complete this step, you'll need
to find a friend with administrative access and ask them for two things:

1) Have them read https://docs.aws.amazon.com/AmazonECS/latest/developerguide/instance_IAM_role.html and explain that you need a role
that can be assumed by EC2 and an instance profile for that role.
2) Have them grant your AWS iam:PassRole permissions to allow you to assign it to instances when you deploy them.

Your friend may be confused, or not care enough - maybe they use Terraform. If so, offer them this snippet and say "This is what I need".
(You, too will know more about Terraform shortly)

```
resource "aws_iam_role" "ecs_instance_role" {
  name = "ecs_instance"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_policy" "pass_ecs_instance_role"{
  name = "pass_ecs_instance_role"
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": "iam:PassRole",
            "Resource": "${aws_iam_role.ecs_instance_role.arn}"
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "ecs_instance_managed_policy" {
  role       = "${aws_iam_role.ecs_instance_role.name}"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"

}

resource "aws_iam_instance_profile" "ecs_instance_profile" {
  name = "ecs_instance_profile"
  role = "${aws_iam_role.ecs_instance_role.name}"
}
``` 

Swell. I hope that worked out well for you.

### 2. Redeploy your EC2 instance with the new instance profile attached


(Replace KEY_NAME, SECURITY_GROUP, and INSTANCE_PROFILE_NAME below)
```
INSTANCE_ID=$(aws ec2 run-instances --image-id ami-02507631a9f7bc956 \ 
  --count 1 \
  --instance-type t2.medium \
  --key-name KEY_NAME \
  --security-group-ids SECURITY_GROUP \
  --iam-instance-profile Name=INSTANCE_PROFILE_NAME \
  --region us-east-1 | jq -r '.Instances[0].InstanceId') 

ssh ec2-user@$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --region us-east-1 | jq -r '.Reservations[0].Instances[0].PublicIpAddress')
```

On the host, just to check that your instances AWS credentials are working, install the aws cli and verify your caller identity:

```
sudo yum install -y aws-cli
aws sts get-caller-identity
```

You should see output like:

```
{
    "Account": "12345", 
    "UserId": "AROAZMU2WYROA7FMHAJUA:i-023de8af2376fbce4", 
    "Arn": "arn:aws:sts::12345:assumed-role/ecs_instance/i-023de8af2376fbce4"
}
```

If you'd like to be double sure that it's working, attempt to pull your docker image directly on the host:

```
$(aws ecr get-login --region us-east-1 --no-include-email)
docker run -p 8080:8080 -it REPOSITORY_URI:v1.0
```

With any luck your web app should start soon. Once satisfied, feel free to terminate the instance. We're going to 
make it so that the webapp starts automatically when the instance boots.

### 3. Configure the instance to run the app on startup

In addition to specifying an IAM role for an instance to assume, we can also specify a script that AWS runs on the host upon
our behalf on start up. This is called the instances' User Data. It's a terrible name for a really cool field.

Generally User Data contains a small bash script. In our case, we want the script to install aws-cli, login to docker, and start
our container.

That would look something like this:

```
cat <<'EOF' >> myfirstuserdata.txt
#!/bin/bash
yum install -y aws-cli
$(aws ecr get-login --region us-east-1 --no-include-email)
docker run -p 8080:8080 -d REPOSITORY_URI:v1.0
EOF
```

We'd pass it to the cli just like this: 

```
aws ec2 run-instances --image-id ami-02507631a9f7bc956 \ 
  --count 1 \
  --instance-type t2.medium \
  --key-name KEY_NAME \
  --security-group-ids SECURITY_GROUP \
  --iam-instance-profile Name=INSTANCE_PROFILE_NAME \
  --user-data file://myfirstuserdata.txt
  --region us-east-1  
```

Go ahead and try it if you'd like. It's fun. Once satisfied, let's move into even more interesting territory and set the

## Congratulations!


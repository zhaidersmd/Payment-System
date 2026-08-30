pipeline {

    agent any

    environment {
        AWS_REGION = 'ap-south-1'

        AWS_ACCOUNT_ID = '774118824657'

        ECR_REPO = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/payment-service"

        IMAGE_TAG = "${BUILD_NUMBER}"

        LAUNCH_TEMPLATE = 'payment-service-lt'

        ASG_NAME = 'payment-service-asg'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh '''
                    ./mvnw clean package -DskipTests
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    set -e

                    echo "======================================"
                    echo "Docker Build"
                    echo "======================================"

                    echo "Docker version:"
                    docker version

                    echo "Buildx version:"
                    docker buildx version

                    echo "Building AMD64 image..."

                    docker buildx build \
                        --platform linux/amd64 \
                        --load \
                        -t ${ECR_REPO}:${IMAGE_TAG} \
                        -t ${ECR_REPO}:latest \
                        .

                    echo "Verifying image platform..."

                    docker image inspect \
                        ${ECR_REPO}:${IMAGE_TAG} \
                        --format '{{.Os}}/{{.Architecture}}'

                    echo "Docker image build completed successfully."
                '''
            }
        }



        stage('ECR Login') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                     credentialsId: 'aws-patient-service']
                ]) {
                    sh '''
                        echo "Logging into Amazon ECR..."

                        aws ecr get-login-password \
                            --region ${AWS_REGION} | \
                        docker login \
                            --username AWS \
                            --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                    '''
                }
            }
        }

        stage('Push Image') {
            steps {
                sh '''
                    echo "Pushing image ${ECR_REPO}:${IMAGE_TAG}"

                    docker push ${ECR_REPO}:${IMAGE_TAG}

                    echo "Pushing latest tag..."

                    docker push ${ECR_REPO}:latest
                '''
            }
        }

        stage('Prepare User Data') {
            steps {
                sh '''
                    set -e

                    echo "Preparing user data for image tag: ${IMAGE_TAG}"

                    sed "s/__IMAGE_TAG__/${IMAGE_TAG}/g" \
                        deployment/user-data.sh \
                        > /tmp/payment-user-data.sh

                    echo "User data prepared successfully."

                    grep "IMAGE_TAG=" /tmp/payment-user-data.sh
                '''
            }
        }

        stage('Create Launch Template Version') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                     credentialsId: 'aws-patient-service']
                ]) {
                    sh '''
                        set -e

                        echo "Encoding user data..."

                        USER_DATA=$(base64 -i /tmp/payment-user-data.sh | tr -d '\\n')

                        echo "Creating new launch template version..."

                        aws ec2 create-launch-template-version \
                            --region ${AWS_REGION} \
                            --launch-template-name ${LAUNCH_TEMPLATE} \
                            --source-version '$Latest' \
                            --version-description "Jenkins build ${BUILD_NUMBER} - image ${IMAGE_TAG}" \
                            --launch-template-data "{\"UserData\":\"${USER_DATA}\"}"

                        echo "Launch template version created successfully."
                    '''
                }
            }
        }

        stage('Update Auto Scaling Group') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                     credentialsId: 'aws-patient-service']
                ]) {
                    sh '''
                        set -e

                        echo "Getting latest launch template version..."

                        LATEST_VERSION=$(aws ec2 describe-launch-template-versions \
                            --region ${AWS_REGION} \
                            --launch-template-name ${LAUNCH_TEMPLATE} \
                            --versions '$Latest' \
                            --query 'LaunchTemplateVersions[0].VersionNumber' \
                            --output text)

                        echo "Latest Launch Template Version: ${LATEST_VERSION}"

                        echo "Updating Auto Scaling Group..."

                        aws autoscaling update-auto-scaling-group \
                            --region ${AWS_REGION} \
                            --auto-scaling-group-name ${ASG_NAME} \
                            --launch-template \
                            LaunchTemplateName=${LAUNCH_TEMPLATE},Version=${LATEST_VERSION}

                        echo "Auto Scaling Group updated successfully."
                    '''
                }
            }
        }

        stage('Start Instance Refresh') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                     credentialsId: 'aws-patient-service']
                ]) {
                    sh '''
                        set -e

                        echo "Starting Auto Scaling Group instance refresh..."

                        aws autoscaling start-instance-refresh \
                            --region ${AWS_REGION} \
                            --auto-scaling-group-name ${ASG_NAME} \
                            --preferences \
                            MinHealthyPercentage=100,InstanceWarmup=120

                        echo "Instance refresh started successfully."
                    '''
                }
            }
        }

        stage('Deployment Status') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                     credentialsId: 'aws-patient-service']
                ]) {
                    sh '''
                        echo "Checking instance refresh status..."

                        aws autoscaling describe-instance-refreshes \
                            --region ${AWS_REGION} \
                            --auto-scaling-group-name ${ASG_NAME} \
                            --max-records 1
                    '''
                }
            }
        }
    }

    post {

        success {
            echo """
            ==========================================
            CI/CD PIPELINE SUCCESSFUL
            ==========================================

            Image:
            ${ECR_REPO}:${IMAGE_TAG}

            Deployment:
            Launch Template -> ASG Instance Refresh

            ==========================================
            """
        }

        failure {
            echo """
            ==========================================
            CI/CD PIPELINE FAILED
            ==========================================

            Check Jenkins console logs.
            ==========================================
            """
        }
    }
}
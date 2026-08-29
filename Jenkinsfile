pipeline {
    agent any

    environment {
        AWS_REGION = 'ap-south-1'

        ECR_REGISTRY = '774118824657.dkr.ecr.ap-south-1.amazonaws.com'
        ECR_REPOSITORY = '774118824657.dkr.ecr.ap-south-1.amazonaws.com/payment-service'

        AWS_CREDENTIALS = 'aws-patient-service'

        IAM_INSTANCE_PROFILE = 'payment-service-ec2-role'

        IMAGE_TAG = "${BUILD_NUMBER}"

        LAUNCH_TEMPLATE_ID = 'lt-0182e0cd846290df8'
        ASG_NAME = 'payment-service-asg'

        HEALTH_URL = 'http://payment-service-alb-779319125.ap-south-1.elb.amazonaws.com/actuator/health'
    }

    stages {

        /*
         * ============================================================
         * CHECKOUT
         * ============================================================
         */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }


        /*
         * ============================================================
         * BUILD & TEST
         * ============================================================
         */
//         stage('Build & Test') {
//             steps {
//                 sh '''
//                     set -e
//
//                     echo "======================================"
//                     echo "Building Payment Service"
//                     echo "======================================"
//
//                     ./mvnw -B clean test
//                 '''
//             }
//         }


        /*
         * ============================================================
         * DOCKER BUILD
         * ============================================================
         */
        stage('Docker Build') {
            steps {

                echo "Building Docker image:"
                echo "${ECR_REPOSITORY}:${IMAGE_TAG}"

                sh '''
                    set -e

                    echo "======================================"
                    echo "Docker Build"
                    echo "======================================"

                    docker build \
                        --platform linux/amd64 \
                        -t ${ECR_REPOSITORY}:${IMAGE_TAG} \
                        .

                    echo "Docker image built successfully."

                    echo "Inspecting Docker image..."

                    docker image inspect \
                        ${ECR_REPOSITORY}:${IMAGE_TAG}

                    echo "Docker image verification successful."
                '''
            }
        }


        /*
         * ============================================================
         * ECR LOGIN
         * ============================================================
         */
        stage('ECR Login') {
            steps {

                withCredentials([
                    [
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIALS}"
                    ]
                ]) {

                    sh '''
                        set -e

                        echo "======================================"
                        echo "AWS ECR Login"
                        echo "======================================"

                        echo "AWS Region: ${AWS_REGION}"
                        echo "ECR Registry: ${ECR_REGISTRY}"

                        aws sts get-caller-identity

                        aws ecr get-login-password \
                            --region ${AWS_REGION} | \
                        docker login \
                            --username AWS \
                            --password-stdin ${ECR_REGISTRY}

                        echo "ECR login successful."
                    '''
                }
            }
        }


        /*
         * ============================================================
         * PUSH IMAGE
         * ============================================================
         */
        stage('Push to ECR') {
            steps {

                sh '''
                    set -e

                    echo "======================================"
                    echo "Push Docker Image"
                    echo "======================================"

                    echo "Image:"
                    echo "${ECR_REPOSITORY}:${IMAGE_TAG}"

                    docker push \
                        ${ECR_REPOSITORY}:${IMAGE_TAG}

                    echo "Image pushed successfully."

                    echo "Verifying image in ECR..."

                    aws ecr describe-images \
                        --repository-name payment-service \
                        --image-ids imageTag=${IMAGE_TAG} \
                        --region ${AWS_REGION}

                    echo "ECR image verification successful."
                '''
            }
        }


        /*
         * ============================================================
         * CREATE LAUNCH TEMPLATE VERSION
         * ============================================================
         */
        stage('Create Launch Template Version') {
            steps {

                withCredentials([
                    [
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIALS}"
                    ]
                ]) {

                    sh '''
                        set -e

                        echo "======================================"
                        echo "Creating Launch Template Version"
                        echo "======================================"

                        echo "Launch Template ID:"
                        echo "${LAUNCH_TEMPLATE_ID}"

                        echo "ASG:"
                        echo "${ASG_NAME}"

                        echo "Image:"
                        echo "${ECR_REPOSITORY}:${IMAGE_TAG}"

                        echo "Build Number:"
                        echo "${BUILD_NUMBER}"

                        echo "IAM Instance Profile:"
                        echo "${IAM_INSTANCE_PROFILE}"


                        # ------------------------------------------------
                        # Validate required variables
                        # ------------------------------------------------

                        if [ -z "${LAUNCH_TEMPLATE_ID}" ]; then
                            echo "ERROR: LAUNCH_TEMPLATE_ID is empty"
                            exit 1
                        fi

                        if [ -z "${IMAGE_TAG}" ]; then
                            echo "ERROR: IMAGE_TAG is empty"
                            exit 1
                        fi

                        if [ -z "${IAM_INSTANCE_PROFILE}" ]; then
                            echo "ERROR: IAM_INSTANCE_PROFILE is empty"
                            exit 1
                        fi


                        # ------------------------------------------------
                        # Verify Launch Template exists
                        # ------------------------------------------------

                        echo "Verifying Launch Template..."

                        aws ec2 describe-launch-templates \
                            --launch-template-ids "${LAUNCH_TEMPLATE_ID}" \
                            --region "${AWS_REGION}"

                        echo "Launch Template exists."


                        # ------------------------------------------------
                        # Generate User Data
                        # ------------------------------------------------

                        echo "======================================"
                        echo "Generating User Data"
                        echo "======================================"

                        if [ ! -f deployment/user-data.sh ]; then
                            echo "ERROR: deployment/user-data.sh not found"
                            exit 1
                        fi

                        sed "s/__IMAGE_TAG__/${IMAGE_TAG}/g" \
                            deployment/user-data.sh \
                            > generated-user-data.sh

                        echo "Generated User Data successfully."

                        echo "Checking generated image tag..."

                        grep "IMAGE_TAG=" generated-user-data.sh || true


                        # ------------------------------------------------
                        # Base64 encode User Data
                        # ------------------------------------------------

                        echo "======================================"
                        echo "Encoding User Data"
                        echo "======================================"

                        USER_DATA=$(base64 -w 0 generated-user-data.sh)

                        if [ -z "${USER_DATA}" ]; then
                            echo "ERROR: User Data encoding failed"
                            exit 1
                        fi

                        echo "User Data encoded successfully."

                        echo "Encoded User Data length:"
                        echo "${#USER_DATA}"


                        # ------------------------------------------------
                        # Generate Launch Template JSON
                        # ------------------------------------------------

                        echo "======================================"
                        echo "Generating Launch Template JSON"
                        echo "======================================"

                        jq -n \
                            --arg userData "${USER_DATA}" \
                            --arg profileName "${IAM_INSTANCE_PROFILE}" \
                            '{
                                UserData: $userData,
                                IamInstanceProfile: {
                                    Name: $profileName
                                }
                            }' \
                            > launch-template-data.json


                        # ------------------------------------------------
                        # Validate JSON
                        # ------------------------------------------------

                        echo "Validating Launch Template JSON..."

                        jq empty launch-template-data.json

                        echo "JSON validation successful."

                        echo "Launch Template data:"

                        jq '{
                            IamInstanceProfile: .IamInstanceProfile,
                            UserDataLength: (.UserData | length)
                        }' launch-template-data.json


                        # ------------------------------------------------
                        # Create Launch Template Version
                        # ------------------------------------------------

                        echo "======================================"
                        echo "Creating New Launch Template Version"
                        echo "======================================"

                        NEW_VERSION=$(aws ec2 create-launch-template-version \
                            --launch-template-id "${LAUNCH_TEMPLATE_ID}" \
                            --source-version '$Default' \
                            --version-description \
                                "Jenkins build ${BUILD_NUMBER} - payment-service image ${IMAGE_TAG}" \
                            --launch-template-data \
                                file://launch-template-data.json \
                            --region "${AWS_REGION}" \
                            --query \
                                'LaunchTemplateVersion.VersionNumber' \
                            --output text)


                        # ------------------------------------------------
                        # Validate new version
                        # ------------------------------------------------

                        if [ -z "${NEW_VERSION}" ] || \
                           [ "${NEW_VERSION}" = "None" ]; then

                            echo "ERROR: Failed to create Launch Template version"
                            exit 1
                        fi

                        echo "======================================"
                        echo "NEW LAUNCH TEMPLATE VERSION"
                        echo "======================================"

                        echo "${NEW_VERSION}"


                        # ------------------------------------------------
                        # Verify newly created version
                        # ------------------------------------------------

                        echo "======================================"
                        echo "Verifying Launch Template Version"
                        echo "======================================"

                        aws ec2 describe-launch-template-versions \
                            --launch-template-id "${LAUNCH_TEMPLATE_ID}" \
                            --versions "${NEW_VERSION}" \
                            --region "${AWS_REGION}" \
                            --query \
                                'LaunchTemplateVersions[0].{
                                    Version:VersionNumber,
                                    DefaultVersion:DefaultVersion,
                                    ImageId:LaunchTemplateData.ImageId,
                                    InstanceType:LaunchTemplateData.InstanceType,
                                    IamInstanceProfile:LaunchTemplateData.IamInstanceProfile
                                }' \
                            --output json


                        # ------------------------------------------------
                        # Save version for next stage
                        # ------------------------------------------------

                        echo "${NEW_VERSION}" \
                            > "${WORKSPACE}/new_lt_version.txt"

                        echo "Launch Template version saved to:"
                        echo "${WORKSPACE}/new_lt_version.txt"

                    '''
                }
            }
        }


        /*
         * ============================================================
         * UPDATE ASG
         * ============================================================
         */
        stage('Update Auto Scaling Group') {
            steps {

                withCredentials([
                    [
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIALS}"
                    ]
                ]) {

                    sh '''
                        set -e

                        echo "======================================"
                        echo "Updating Auto Scaling Group"
                        echo "======================================"

                        if [ ! -f "${WORKSPACE}/new_lt_version.txt" ]; then
                            echo "ERROR: new_lt_version.txt not found"
                            exit 1
                        fi

                        NEW_VERSION=$(tr -d '[:space:]' \
                            < "${WORKSPACE}/new_lt_version.txt")


                        if [ -z "${NEW_VERSION}" ]; then
                            echo "ERROR: NEW_VERSION is empty"
                            exit 1
                        fi


                        echo "ASG:"
                        echo "${ASG_NAME}"

                        echo "Launch Template:"
                        echo "${LAUNCH_TEMPLATE_ID}"

                        echo "New Version:"
                        echo "${NEW_VERSION}"


                        # ------------------------------------------------
                        # Update ASG
                        # ------------------------------------------------

                        aws autoscaling update-auto-scaling-group \
                            --auto-scaling-group-name "${ASG_NAME}" \
                            --launch-template \
                                LaunchTemplateId="${LAUNCH_TEMPLATE_ID}",Version="${NEW_VERSION}" \
                            --region "${AWS_REGION}"


                        echo "ASG updated successfully."


                        # ------------------------------------------------
                        # Verify ASG configuration
                        # ------------------------------------------------

                        echo "======================================"
                        echo "Verifying ASG Configuration"
                        echo "======================================"

                        aws autoscaling describe-auto-scaling-groups \
                            --auto-scaling-group-names "${ASG_NAME}" \
                            --region "${AWS_REGION}" \
                            --query \
                                'AutoScalingGroups[0].LaunchTemplate' \
                            --output json

                    '''
                }
            }
        }


        /*
         * ============================================================
         * START INSTANCE REFRESH
         * ============================================================
         */
        stage('Start Instance Refresh') {
            steps {

                withCredentials([
                    [
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIALS}"
                    ]
                ]) {

                    sh '''
                        set -e

                        echo "======================================"
                        echo "Starting Instance Refresh"
                        echo "======================================"

                        NEW_VERSION=$(tr -d '[:space:]' \
                            < "${WORKSPACE}/new_lt_version.txt")


                        echo "ASG:"
                        echo "${ASG_NAME}"

                        echo "Launch Template Version:"
                        echo "${NEW_VERSION}"


                        # ------------------------------------------------
                        # Start refresh
                        # ------------------------------------------------

                        REFRESH_ID=$(aws autoscaling start-instance-refresh \
                            --auto-scaling-group-name "${ASG_NAME}" \
                            --preferences \
                                MinHealthyPercentage=0,MaxHealthyPercentage=100,InstanceWarmup=300 \
                            --region "${AWS_REGION}" \
                            --query 'InstanceRefreshId' \
                            --output text)


                        if [ -z "${REFRESH_ID}" ] || \
                           [ "${REFRESH_ID}" = "None" ]; then

                            echo "ERROR: Instance Refresh was not created"
                            exit 1
                        fi


                        echo "======================================"
                        echo "INSTANCE REFRESH STARTED"
                        echo "======================================"

                        echo "Refresh ID:"
                        echo "${REFRESH_ID}"


                        # ------------------------------------------------
                        # Save refresh ID
                        # ------------------------------------------------

                        echo "${REFRESH_ID}" \
                            > "${WORKSPACE}/refresh_id.txt"

                    '''
                }
            }
        }


        /*
         * ============================================================
         * WAIT FOR INSTANCE REFRESH
         * ============================================================
         */
        stage('Wait For Deployment') {
            steps {

                withCredentials([
                    [
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${AWS_CREDENTIALS}"
                    ]
                ]) {

                    sh '''
                        set -e

                        echo "======================================"
                        echo "Waiting For Instance Refresh"
                        echo "======================================"


                        if [ ! -f "${WORKSPACE}/refresh_id.txt" ]; then
                            echo "ERROR: refresh_id.txt not found"
                            exit 1
                        fi


                        REFRESH_ID=$(tr -d '[:space:]' \
                            < "${WORKSPACE}/refresh_id.txt")


                        echo "Refresh ID:"
                        echo "${REFRESH_ID}"


                        # ------------------------------------------------
                        # Poll refresh status
                        # ------------------------------------------------

                        for i in $(seq 1 60); do

                            STATUS=$(aws autoscaling describe-instance-refreshes \
                                --auto-scaling-group-name "${ASG_NAME}" \
                                --instance-refresh-ids "${REFRESH_ID}" \
                                --region "${AWS_REGION}" \
                                --query \
                                    'InstanceRefreshes[0].Status' \
                                --output text)


                            PERCENTAGE=$(aws autoscaling describe-instance-refreshes \
                                --auto-scaling-group-name "${ASG_NAME}" \
                                --instance-refresh-ids "${REFRESH_ID}" \
                                --region "${AWS_REGION}" \
                                --query \
                                    'InstanceRefreshes[0].PercentageComplete' \
                                --output text)


                            echo "--------------------------------------"
                            echo "Attempt: ${i}/60"
                            echo "Status: ${STATUS}"
                            echo "Progress: ${PERCENTAGE}%"
                            echo "--------------------------------------"


                            case "${STATUS}" in

                                Successful)
                                    echo "======================================"
                                    echo "INSTANCE REFRESH SUCCESSFUL"
                                    echo "======================================"
                                    exit 0
                                    ;;


                                Failed|Cancelled|RollbackFailed)
                                    echo "======================================"
                                    echo "INSTANCE REFRESH FAILED"
                                    echo "======================================"

                                    echo "Status:"
                                    echo "${STATUS}"

                                    exit 1
                                    ;;


                                Pending|InProgress)
                                    echo "Deployment still in progress..."
                                    sleep 20
                                    ;;


                                *)
                                    echo "Unknown refresh status:"
                                    echo "${STATUS}"

                                    sleep 20
                                    ;;

                            esac

                        done


                        echo "======================================"
                        echo "DEPLOYMENT TIMEOUT"
                        echo "======================================"

                        exit 1

                    '''
                }
            }
        }


        /*
         * ============================================================
         * VERIFY DEPLOYMENT
         * ============================================================
         */
        stage('Verify Deployment') {
            steps {

                script {

                    timeout(time: 5, unit: 'MINUTES') {

                        waitUntil {

                            def status = sh(
                                script: """
                                    curl -s \
                                        --connect-timeout 5 \
                                        --max-time 10 \
                                        -o /tmp/payment-health.json \
                                        -w "%{http_code}" \
                                        ${HEALTH_URL}
                                """,
                                returnStdout: true
                            ).trim()


                            def response = sh(
                                script: '''
                                    if [ -f /tmp/payment-health.json ]; then
                                        cat /tmp/payment-health.json
                                    else
                                        echo "No health response"
                                    fi
                                ''',
                                returnStdout: true
                            ).trim()


                            echo "======================================"
                            echo "Payment Service Health Check"
                            echo "======================================"

                            echo "Health URL:"
                            echo "${HEALTH_URL}"

                            echo "HTTP Status:"
                            echo "${status}"

                            echo "Health Response:"
                            echo "${response}"


                            if (
                                status == '200' &&
                                response.contains('"status":"UP"')
                            ) {

                                echo "======================================"
                                echo "APPLICATION HEALTH CHECK PASSED"
                                echo "======================================"

                                return true
                            }


                            echo "Application is not healthy yet."

                            echo "Waiting 10 seconds..."

                            sleep 10

                            return false
                        }
                    }
                }
            }
        }
    }


    /*
     * ================================================================
     * POST ACTIONS
     * ================================================================
     */
    post {

        success {

            echo "======================================"
            echo "CI/CD PIPELINE SUCCESS"
            echo "======================================"

            echo "Service:"
            echo "payment-service"

            echo "Image:"
            echo "${ECR_REPOSITORY}:${IMAGE_TAG}"

            echo "Launch Template:"
            echo "${LAUNCH_TEMPLATE_ID}"

            echo "ASG:"
            echo "${ASG_NAME}"

            echo "Deployment completed successfully."
        }


        failure {

            echo "======================================"
            echo "CI/CD PIPELINE FAILED"
            echo "======================================"

            echo "Service:"
            echo "payment-service"

            echo "Build:"
            echo "${BUILD_NUMBER}"

            echo "Check the failed stage above for details."
        }


        always {

            sh '''
                docker logout ${ECR_REGISTRY} || true
            '''
        }
    }
}

// docker run -d \
//   --name jenkins \
//   -p 8080:8080 \
//   -p 50000:50000 \
//   -v jenkins_home:/var/jenkins_home \
//   -v ~/.docker/run/docker.sock:/var/run/docker.sock \
//   --user root \
//   my-jenkins:lts

pipeline {

    agent any

    environment {
            AWS_REGION = 'ap-south-1'
            ECR_REGISTRY = '774118824657.dkr.ecr.ap-south-1.amazonaws.com'
            ECR_REPO = '774118824657.dkr.ecr.ap-south-1.amazonaws.com/payment-service'
            IMAGE_TAG = "${BUILD_NUMBER}"
        }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './mvnw clean compile'
            }
        }

//         stage('Test') {
//             steps {
//                 sh './mvnw test'
//             }
//         }

//         stage('Stop Existing Spring Boot App') {
//             steps {
//                 sh '''
//                     echo "Checking for existing application on port 4000..."
//
//                     if [ -f payment-service.pid ]; then
//                         PID=$(cat payment-service.pid)
//
//                         if kill -0 "$PID" 2>/dev/null; then
//                             echo "Stopping existing application PID: $PID"
//                             kill "$PID" || true
//                             sleep 3
//                         fi
//
//                         rm -f payment-service.pid
//                     fi
//
//                     PID=$(lsof -ti :4000 || true)
//
//                     if [ -n "$PID" ]; then
//                         echo "Port 4000 is still in use by: $PID"
//
//                         kill $PID || true
//                         sleep 3
//
//                         PID=$(lsof -ti :4000 || true)
//
//                         if [ -n "$PID" ]; then
//                             echo "Force killing: $PID"
//                             kill -9 $PID || true
//                         fi
//                     fi
//
//                     echo "Port 4000 is available."
//                 '''
//             }
//         }


//         stage('Package & Run') {
//             steps {
//                 sh '''
//                     ./mvnw clean package -DskipTests
//
//                     JAR=$(find target -maxdepth 1 -name 'payment-service-*.jar' ! -name '*original*.jar' | head -n 1)
//                     echo "Starting $JAR"
//
//                     export JENKINS_NODE_COOKIE=dontKillMe
//                     export DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/paymentdb"
//                     export KAFKA_URL="host.docker.internal:9002"
//                     export REDIS_URL="host.docker.internal"
//
//                     echo "DATASOURCE_URL=$DATASOURCE_URL"
//                     echo "KAFKA_URL=$KAFKA_URL"
//                     echo "REDIS_URL=$REDIS_URL"
//
//                     nohup env \
//                         DATASOURCE_URL="$DATASOURCE_URL" \
//                         KAFKA_URL="$KAFKA_URL" \
//                         REDIS_URL="$REDIS_URL" \
//                         java -jar "$JAR" \
//                         > payment-service.log 2>&1 &
//
//                     echo $! > payment-service.pid
//                     sleep 5
//                     cat payment-service.log
//
//                 '''
//             }
//         }

stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                        -t ${ECR_REPO}:${IMAGE_TAG} \
                        -t ${ECR_REPO}:latest \
                        .
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
                                aws ecr get-login-password \
                                  --region ${AWS_REGION} | \
                                docker login \
                                  --username AWS \
                                  --password-stdin ${ECR_REGISTRY}
                            '''
                        }
                    }
                }

                stage('Push Image') {
                            steps {
                                sh '''
                                    docker push ${ECR_REPO}:${IMAGE_TAG}
                                    docker push ${ECR_REPO}:latest
                                '''
                            }
                        }

                        stage('Deploy') {
                            steps {
                                withCredentials([
                                    [$class: 'AmazonWebServicesCredentialsBinding',
                                     credentialsId: 'aws-patient-service']
                                ]) {
                                    sh '''
                                        echo "Deployment image: ${ECR_REPO}:${IMAGE_TAG}"
                                    '''
                                }
                            }
                        }
                    }
                

  post {

          success {
              echo 'Payment Service CI pipeline completed successfully.'
          }

          failure {
              echo 'Payment Service CI pipeline failed.'
          }

      }

    }

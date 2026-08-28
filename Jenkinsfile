pipeline {

    agent any

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

        stage('Test') {
            steps {
                sh './mvnw test'
            }
        }

        stage('Stop Existing Spring Boot App') {
                    steps {
                        sh '''
                            echo "Checking for existing application on port 4000..."

                            PID=$(lsof -ti :4000 || true)

                            if [ -n "$PID" ]; then
                                echo "Found process on port 4000: $PID"
                                echo "Stopping application..."

                                kill $PID || true

                                sleep 3

                                # If it is still running, force kill it
                                if kill -0 $PID 2>/dev/null; then
                                    echo "Process is still running. Force killing..."
                                    kill -9 $PID || true
                                fi

                                echo "Existing application stopped."
                            else
                                echo "No application running on port 4000."
                            fi
                        '''
                    }
                }

        stage('Package & Run') {
            steps {
                sh '''
                    ./mvnw clean package -DskipTests

                    JAR=$(find target -maxdepth 1 -name 'payment-service-*.jar' ! -name '*original*.jar' | head -n 1)
                    echo "Starting $JAR"

                    JENKINS_NODE_COOKIE=dontKillMe \
                                nohup java -jar "$JAR" \
                                    --spring.datasource.url=jdbc:postgresql://host.docker.internal:5432/paymentdb \
                                    > payment-service.log 2>&1 &

                    echo $! > payment-service.pid
                    sleep 5
                    cat payment-service.log

                '''
            }
        }

    }
}
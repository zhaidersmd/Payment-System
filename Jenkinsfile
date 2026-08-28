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

                    if [ -f payment-service.pid ]; then
                        PID=$(cat payment-service.pid)

                        if kill -0 "$PID" 2>/dev/null; then
                            echo "Stopping existing application PID: $PID"
                            kill "$PID" || true
                            sleep 3
                        fi

                        rm -f payment-service.pid
                    fi

                    PID=$(lsof -ti :4000 || true)

                    if [ -n "$PID" ]; then
                        echo "Port 4000 is still in use by: $PID"

                        kill $PID || true
                        sleep 3

                        PID=$(lsof -ti :4000 || true)

                        if [ -n "$PID" ]; then
                            echo "Force killing: $PID"
                            kill -9 $PID || true
                        fi
                    fi

                    echo "Port 4000 is available."
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
                                    DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/paymentdb" \
                                    KAFKA_URL="host.docker.internal:9002" \
                                    REDIS_URL="host.docker.internal" \
                                    > payment-service.log 2>&1 &

                    echo $! > payment-service.pid
                    sleep 5
                    cat payment-service.log

                '''
            }
        }

    }
}
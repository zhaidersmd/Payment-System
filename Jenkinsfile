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
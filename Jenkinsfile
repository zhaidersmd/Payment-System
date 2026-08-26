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

                    nohup java -jar target/payment-service-0.0.1-SNAPSHOT.jar \
                        > payment-service.log 2>&1 &

                    echo $! > payment-service.pid
                '''
            }
        }

    }
}
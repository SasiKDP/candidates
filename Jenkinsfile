pipeline {
    agent any
    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKERHUB_REPO = "sandycis476"
        IMAGE_NAME = "candidates"
        KUBE_NAMESPACE = "ingress-nginx"
        DOCKER_CREDENTIALS_ID = "docker-hub"
        KUBECONFIG_CREDENTIALS_ID = "k8s-service-account-token"
        GOOGLE_CREDENTIALS = 'gcloud-credentials-id' // Jenkins credentials ID for Google Cloud JSON key
        GCP_PROJECT = 'proud-outpost-447109-m8'
        GKE_CLUSTER = 'k8s-dataquad-dev'
        GKE_ZONE = 'us-central1-c' // e.g., us-central1-a
    }
    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'master', 
                url: 'https://github.com/NaveenKumar-dataquad/Dstaquad-Candidate.git'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                    docker build -t ${DOCKERHUB_REPO}/${IMAGE_NAME}:${BUILD_ID} .
                    """
                }
            }
        }
        
        
        stage('Push to Docker Hub') {
            steps {
                script {
                    sh """
                    echo $DOCKERHUB_REPO/$IMAGE_NAME:$BUILD_ID
                    docker login -u ${env.DOCKERHUB_REPO} -p Appy@1988
                    docker push ${DOCKERHUB_REPO}/${IMAGE_NAME}:${BUILD_ID}
                    docker tag ${DOCKERHUB_REPO}/${IMAGE_NAME}:${BUILD_ID} ${DOCKERHUB_REPO}/${IMAGE_NAME}:latest
                    docker push ${DOCKERHUB_REPO}/${IMAGE_NAME}:latest
                    """
                }
            }
        }
        
        stage('Update K8s Manifests') {
            steps {
                script {
                    sh """
                        sed -i 's|image:.*|image: ${DOCKERHUB_REPO}/${IMAGE_NAME}:${env.BUILD_ID}|g' k8s/deployment.yaml
                    """
                }
            }
        }

         stage('Authenticate with GCP') {
            steps {
                script {
                    withCredentials([file(credentialsId: "${env.GOOGLE_CREDENTIALS}", variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                        sh """
                        gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                        gcloud config set project ${env.GCP_PROJECT}
                        """
                    }
                }
            }
        } 
        stage('Configure kubectl') {
            steps {
                script {
                    sh """
                    gcloud container clusters get-credentials ${env.GKE_CLUSTER} --zone ${env.GKE_ZONE} --project ${env.GCP_PROJECT}
                    """
                }
            }
        }
        stage('Apply Kubernetes Manifest') {
            steps {
                sh """
                kubectl apply -f k8s/deployment.yaml -n ingress-nginx
                """
            }
        }
    }
    post {
        success {
            echo 'Deployment succeeded!'
        }
        failure {
            echo 'Deployment failed. Check the logs for details.'
        }
    }    
}

Write-Host "Cleaning up payment service in namespace microservice-network..." -ForegroundColor Yellow

kubectl delete deployment payment-service -n microservice-network --ignore-not-found
kubectl delete deployment payment-mongodb -n microservice-network --ignore-not-found
kubectl delete deployment zookeeper -n microservice-network --ignore-not-found
kubectl delete deployment kafka -n microservice-network --ignore-not-found

kubectl delete service payment-service -n microservice-network --ignore-not-found
kubectl delete service payment-mongodb -n microservice-network --ignore-not-found
kubectl delete service zookeeper -n microservice-network --ignore-not-found
kubectl delete service kafka -n microservice-network --ignore-not-found

kubectl delete pvc payment-mongodb-pvc -n microservice-network --ignore-not-found

kubectl delete configmap payment-service-config -n microservice-network --ignore-not-found

kubectl delete secret zookeeper-secret -n microservice-network --ignore-not-found
kubectl delete secret payment-mongodb-secret -n microservice-network --ignore-not-found

Write-Host "Cleanup complete!" -ForegroundColor Green
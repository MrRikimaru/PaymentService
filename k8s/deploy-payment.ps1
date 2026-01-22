Write-Host "Deploying payment service in namespace microservice-network..." -ForegroundColor Green

Write-Host "1. Creating namespace..." -ForegroundColor Green
kubectl apply -f namespace.yaml

Write-Host "2. Creating secrets..." -ForegroundColor Green
kubectl apply -f payment-service-secrets.yaml

Write-Host "3. Creating storage..." -ForegroundColor Green
kubectl apply -f payment-mongodb-pvc.yaml

Write-Host "4. Creating databases and message broker..." -ForegroundColor Green
kubectl apply -f payment-mongodb-deployment.yaml
kubectl apply -f zookeeper-deployment.yaml
kubectl apply -f kafka-deployment.yaml

Write-Host "5. Waiting for databases to be ready..." -ForegroundColor Yellow
kubectl wait --namespace=microservice-network --for=condition=ready pod -l app=payment-mongodb --timeout=180s
kubectl wait --namespace=microservice-network --for=condition=ready pod -l app=zookeeper --timeout=120s
kubectl wait --namespace=microservice-network --for=condition=ready pod -l app=kafka --timeout=120s

Write-Host "6. Creating configmap..." -ForegroundColor Green
kubectl apply -f payment-configmap.yaml

Write-Host "7. Waiting 30 seconds for database initialization..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "8. Deploying payment service..." -ForegroundColor Green
kubectl apply -f payment-service-deployment.yaml

Write-Host "9. Waiting for payment service to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10
kubectl wait --namespace=microservice-network --for=condition=ready pod -l app=payment-service --timeout=300s

Write-Host "DEPLOYMENT COMPLETE!" -ForegroundColor Green
Write-Host "To access the service:" -ForegroundColor Yellow
Write-Host "  Use port-forward: kubectl port-forward svc/payment-service 8084:8084 -n microservice-network" -ForegroundColor Cyan
Write-Host "  Then access via: http://localhost:8084" -ForegroundColor Cyan
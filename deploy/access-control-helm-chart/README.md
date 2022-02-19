# access-control-helm-chart


## Build docker images:
1) eval $(minikube docker-env)
   (set environment variables to use docker daemon from minikube)
2) sbt
   (run sbt from root access-control project)
3) project access-control-admin-ws-api-impl
   (move to access-control-admin-ws-api-impl project)
4) docker:publishLocal
   (build image for access-control-admin-ws-api-impl)
5) project access-control-api-impl
6) docker:publishLocal


### Use dive cli tool for analyze images (https://github.com/wagoodman/dive)
1) docker pull wagoodman/dive
2) docker run --rm -it -v /var/run/docker.sock:/var/run/docker.sock wagoodman/dive:latest access-control-admin:0.0.1


## Run Kubernetes cluster:
1) minikube start
2) helm repo add strimzi-kafka-operator https://strimzi.io/charts/
   helm repo add cassandra https://charts.bitnami.com/bitnami/
   helm dependency build ./deploy/access-control-helm-chart
   (import dependencies - kafka, cassandra)
3) helm install --set image.repository=access-control-admin --set image.tag=0.0.1 access-control ./deploy/access-control-helm-chart
   (install local helm chart, set image.tag to actual)
4) helm uninstall access-control
   (clean cluster)


## Inspect cluster:
1) kubectl get pods
   (check pods list)
2) kubectl describe pods <pod-name>
3) kubectl get ing
   (check ingress)
4) kubectl describe ing <ing-name>
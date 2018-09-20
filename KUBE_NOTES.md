# Snowball on Kube on GCP

This file is a list of steps I had to take to get Snowball running on Google Cloud Platform through their managed Kubernetes infrastructure. It's partially for my future reference but it may help you out if you're trying to do something similar.

```bash
# Basic settings
gcloud config set project snowball-[redacted]
gcloud config set compute/zone europe-west4-b

# Create a minimal cluster
gcloud container clusters create snowball-cluster --num-nodes=1 --preemptible

# Configure kubectl for the new cluster
gcloud container clusters get-credentials snowball-cluster 

# Add the config to the new cluster
# Make sure you've created config/config.edn and config/google.json first!
kubectl create configmap snowball-config --from-file config/

# You can update that config with this
# I suppose you could also just delete it and re-create it too
kubectl create configmap snowball-config --from-file config/ -o yaml --dry-run | kubectl replace -f -

# Deploy the container
kubectl run snowball --image=olical/snowball

# Update the deployment and add the YAML below, it maps the config into the container
kubectl edit deployments snowball

# Check the logs with
kubectl logs deployment/snowball -f
```

## YAML for `kubectl edit deployments snowball`

We add `volumeMounts` and `volumes`.

```yaml
    spec:
      containers:
      - image: olical/snowball
        imagePullPolicy: Always
        name: snowball
        resources: {}
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        volumeMounts:
        - name: config-volume
          mountPath: /usr/snowball/config
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
      volumes:
        - name: config-volume
          configMap:
            name: snowball-config
```

# Snowball on GCP via Kube

This file is a list of steps I had to take to get Snowball running on Google Cloud Platform through their managed Kubernetes infrastructure. It's partially for my future reference but it may help you out if you're trying to do something similar.

```bash
# Basic settings
gcloud config set project snowball-[redacted]
gcloud config set compute/zone europe-west4-b

# Create a minimal cluster
# TODO: Use a smaller disk
gcloud container clusters create snowball-cluster --num-nodes=1 --disk-size=20 --preemptible

# Configure kubectl for the new cluster
gcloud container clusters get-credentials snowball-cluster 

# Add the config to the new cluster
# Make sure you've created config/config.edn and config/google.json first!
kubectl create configmap snowball-config --from-file config/

# You can update that config with this
# I suppose you could also just delete it and re-create it too
kubectl create configmap snowball-config --from-file config/ -o yaml --dry-run | kubectl replace -f -

# Deploy the container
kubectl run snowball --image=olical/snowball:...

# Update the deployment and add the YAML below, it maps the config into the container
# I've also set some lower CPU requirements in this YAML to get them to fit
kubectl edit deployments snowball

# Check the logs with this
kubectl logs deployment/snowball -f

# Update to another sha with this
kubectl set image deployment/snowball snowball=olical/snowball:...
```

## YAML for `kubectl edit deployments snowball`

We add `volumeMounts` and `volumes`.

```yaml
    spec:
      containers:
      - image: olical/snowball:...
        imagePullPolicy: IfNotPresent
        name: snowball
        resources: {}
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        volumeMounts:
        - name: config-volume
          mountPath: /usr/snowball/config
        resources:
          requests:
            cpu: "50m"
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

## MusicBot

The music commands are designed for [MusicBot][], you can run that on your Kubernetes cluster too.

```bash
# Create some configuration
# You must create musicbot-config/options.ini and musicbot-config/permissions.ini
kubectl create configmap musicbot-config --from-file musicbot-config/
kubectl create configmap musicbot-i18n --from-file musicbot-config/i18n

# Deploy the container
kubectl run musicbot --image=justsomebots/musicbot:review

# Update the YAML just like snowball and add the new config map, example below
kubectl edit deployments musicbot
```

## MusicBot deployment YAML for config mounting

```yaml
    spec:
      containers:
      - image: justsomebots/musicbot:review
        imagePullPolicy: IfNotPresent
        name: musicbot
        resources: {}
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        volumeMounts:
        - name: config-volume
          mountPath: /usr/src/musicbot/config
        - name: i18n-volume
          mountPath: /usr/src/musicbot/config/i18n
        resources:
          requests:
            cpu: "50m"
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
      volumes:
        - name: config-volume
          configMap:
            name: musicbot-config
        - name: i18n-volume
          configMap:
            name: musicbot-i18n
```

[MusicBot]: https://github.com/Just-Some-Bots/MusicBot

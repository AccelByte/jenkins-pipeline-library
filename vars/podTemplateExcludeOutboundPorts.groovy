// builder -- > builder
// superbuilder -- > builderwithmounteddockersock
// kubectl -- > kubectl
// debian -- > plain debian slim
// default --> docker with bash

def call(Map args = [:], Closure body) {
  if (args.name == null || args.type == null || args.ports == null) return
  def podLabel = "${args.name}-${UUID.randomUUID().toString()}"
  def excludeOutboundPorts = "${args.ports}"
  switch(args.type) {

    // CASE BUILDER ONLY
    case "builder":
      podTemplate(name: podLabel,
        label: podLabel,
        namespace: "jenkins",
        annotations: [
          podAnnotation(key: "traffic.sidecar.istio.io/excludeOutboundPorts", value: excludeOutboundPorts)
        ],
        yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: jenkins-slave
spec:
  containers:
    - name: builder
      image: accelbyte/golang-builder:1.12-alpine3.9
      imagePullPolicy: IfNotPresent
      securityContext:
        privileged: true
      command:
      - cat
      tty: true
      resources:
        limits:
          cpu: 2000m
          memory: 4Gi
        requests:
          cpu: 2000m
          memory: 4Gi
  nodeSelector:
    kubernetes.io/os: linux
""" ){ node(podLabel) { body() } }
      break

    // CASE KUBECTL
    case "kubectl":
    podTemplate(name: podLabel,
      label: podLabel,
      namespace: "jenkins",
      annotations: [
        podAnnotation(key: "traffic.sidecar.istio.io/excludeOutboundPorts", value: excludeOutboundPorts)
      ],
      yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: jenkins-slave
spec:
  containers:
    - name: kubectl
      image: accelbyte/kubectl:v1.17.0
      imagePullPolicy: IfNotPresent
      securityContext:
        privileged: true
      command:
      - cat
      tty: true
      resources:
        limits:
          cpu: 200m
        requests:
          cpu: 200m
  nodeSelector:
    kubernetes.io/os: linux
""" ){ node(podLabel) { body() } }
      break

    // CASE SUPERBUILDER, MOUNT DOCKER SOCK
    case "superbuilder":
      podTemplate(name: podLabel,
        label: podLabel,
        namespace: "jenkins",
        annotations: [
          podAnnotation(key: "traffic.sidecar.istio.io/excludeOutboundPorts", value: excludeOutboundPorts)
        ],
        yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: jenkins-slave
spec:
  securityContext:
    privileged: true
    runAsUser: 0
    fsGroup: 0
  containers:
    - name: jnlp
      image: jenkins/jnlp-slave:alpine
      securityContext:
        privileged: true
        runAsUser: 0
        fsGroup: 0
      tty: true
      volumeMounts:
        - name: dockersock
          mountPath: "/var/run/docker.sock"
        - name: workspace
          mountPath: "/home/jenkins/agent/workspace/${env.JOB_NAME}"
    - name: builder
      image: accelbyte/golang-builder:1.12-alpine3.9
      imagePullPolicy: IfNotPresent
      volumeMounts:
        - name: dockersock
          mountPath: "/var/run/docker.sock"
        - name: workspace
          mountPath: "/home/jenkins/agent/workspace/${env.JOB_NAME}"
      securityContext:
        privileged: true
        runAsUser: 0
        fsGroup: 0
      command:
      - cat
      tty: true
      resources:
        limits:
          cpu: 2000m
          memory: 4Gi
        requests:
          cpu: 2000m
          memory: 4Gi
  volumes:
  - name: dockersock
    hostPath:
      path: /var/run/docker.sock
  - name: workspace
    hostPath:
      path: /home/jenkins/agent/workspace/${env.JOB_NAME}
      mode: 777
  nodeSelector:
    kubernetes.io/os: linux
""" ){ node(podLabel) { body() } }
      break

    // CASE DEBIAN
    case "debian":
    podTemplate(name: podLabel,
      label: podLabel,
      namespace: "jenkins",
      annotations: [
        podAnnotation(key: "traffic.sidecar.istio.io/excludeOutboundPorts", value: excludeOutboundPorts)
      ],
      yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: jenkins-slave
spec:
  containers:
    - name: debian
      image: debian
      imagePullPolicy: IfNotPresent
      securityContext:
        privileged: true
      command:
      - cat
      tty: true
      resources:
        limits:
          cpu: 500m
        requests:
          cpu: 500m
  nodeSelector:
    kubernetes.io/os: linux
""" ){ node(podLabel) { body() } }
      break


    // DOCKER WITH BASH
    default:
    podTemplate(name: podLabel,
      label: podLabel,
      namespace: "jenkins",
      annotations: [
        podAnnotation(key: "traffic.sidecar.istio.io/excludeOutboundPorts", value: excludeOutboundPorts)
      ],
      yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: jenkins-slave
spec:
  containers:
    - name: default
      image: rmwpl/docker-dind-bash-rsync
      imagePullPolicy: IfNotPresent
      securityContext:
        privileged: true
      command:
      - cat
      tty: true
      resources:
        limits:
          cpu: 500m
        requests:
          cpu: 500m
  nodeSelector:
    kubernetes.io/os: linux
""" ){ node(podLabel) { body() } }
      break
  }
}

## Build MOA Docker images locally:

You should login to [the container registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)


Build latest image 
```bash
$ make build-latest
```
Build devel image 
```bash
$ make build-devel
```
Push latest image 
```bash
$ make push-latest
```
Push devel image 
```bash
$ make push-devel
```
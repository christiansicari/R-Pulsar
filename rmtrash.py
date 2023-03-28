import os
import shutil
allowed = ["target",".classpath","DevelopSteps.md","docker-compose.yaml","Documentation.html",".idea","notify_interest","propertyFiles","Rutgers-Pulsar.doxyfile","stormJAR"
           ,"data","docker-compose-smoke-faas.yaml","Dockerfile",".git","LICENSE","pom.xml","pywaggle-logs",".settings","bin","DeploySteps.md","docker-compose-smoke-overlay.yaml","docs",".gitignore","notify_data",".project","README.md","src", "rmtrash.py"]

stage = []
for filename in os.listdir("."):
    if filename not in allowed:
        print(filename)
        stage.append(filename)

if len(stage)>0:
    i = input("Remove all [Y]?")
    if i.lower() == "y":
        for filename in stage:
            if os.path.isdir(filename):
                shutil.rmtree(filename)
            else:
                os.remove(filename)
else:
    print("nothing to do")
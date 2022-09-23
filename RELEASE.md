# Release instructions

## Prerequisites

* use Java 9+ for making release. Make sure the JAVA_HOME environment
  variable is set to the correct JDK directory and exported in your
  terminal
  
* make sure the `pdflatex` program is installed:

    ```
    sudo apt-get install texlive-full
    ```

* ensure that all `pom.xml` files list the same version, update them if 
  necessary to `yyyy.mm.0-SNAPSHOT` (check correct patch level)

* in the moa.core.Globals class, update the versionString variable to
  match the upcoming version
  
* in the `weka-package/Description.props` file, ensure that the following
  properties have been updated and aligned with the upcoming version:
  
    * `Version` (yyyy.mm.0, check correct patch level)
    * `Date`
    * `PackageURL`

* ensure that all code has been committed/pushed


## Deploy to Maven Central

* Ensure your GPG signing key and Sonatype credentials are available in your
  Maven settings.xml file (found at either ~/.m2/ or /usr/share/maven/conf/).
  You can check the settings have been applied correctly by running the command:
  
   ```
   mvn help:effective-settings
   ```
   
* For more information, follow the instructions at the following pages:
  
  https://blog.sonatype.com/2010/01/how-to-generate-pgp-signatures-with-maven/
  https://blog.sonatype.com/2010/11/what-to-do-when-nexus-returns-401/
  
* Publish your public GPG signing key to the keyserver at keyserver.ubuntu.com

* Run the following maven command (from the top-level MOA directory), which
  will automatically use the current version present in the `pom.xml` files
  and then increment it after the release has succeeded.

    ```
    mvn --batch-mode release:prepare release:perform
    ```

* Log into [https://oss.sonatype.org](https://oss.sonatype.org)

* Select **Staging Repositories**, scroll right to the bottom of the list
  and look for a repository called something like *nzacwaikatocmsmoa-XYZ*.
  Select this repository via the check-box.

* Subsequently **Close** and then **Release** the artifacts. NB: It may take a
  few minutes before the *Release* button becomes available, as the system
  is flagging all the artifacts from the staging repo. When releasing, leave
  the **Drop Repository** check-box ticked.

* Perform a `git push`

* Check the following URL after 15-20min (sync with Maven Central only happens 
  every 15min or so) to see whether they artifacts are indeed available from 
  Maven Central (the search index at https://search.maven.org/ only gets updated
  every few hours):
  
  https://repo1.maven.org/maven2/nz/ac/waikato/cms/moa/


## Generate release files

### MOA

* update the *parent version* in `release.xml` to the just released version,
  i.e., `yyyy.mm.0` (without the `-SNAPSHOT` suffix, check correct patch level)

* execute the following commands (top-level directory)

    ```
    mvn clean install -DskipTests=true latex:latex
    mvn -f release.xml prepare-package deb:package install
    ```
    
### Weka Package    
    
* go into the `weka-package` directory 

* update the *parent version* in `update_libs.xml` to the just released version,
  i.e., `yyyy.mm.0` (without the `-SNAPSHOT` suffix, check correct patch level)

* execute the following command to update libraries in the `lib` directory

   ```
   mvn -f update_libs.xml clean package
   ```

* execute the following command (replace `X.Y.Z` with actual version of MOA, 
  eg `yyyy.mm.0`, check correct patch level)

    ```
    ant -f build_package.xml -Dpackage=massiveOnlineAnalysis-X.Y.Z clean make_package
    ```

## Publish release
    
* create new release tag on Github (tag version `yyyy.mm.0`, release title `MOA yy.mm.0`) 
  and upload the generated MOA release zip file from the top-level `target` directory 
  and the zip file from the `weka-package/dist` directory

* make sure that the Github action `Publish MOA Docker image` is triggered and ran successfully.

* email Mark Hall (mhall at waikato.ac.nz) the link to the Weka package zip
  file to upload to the central Weka package repository on Sourceforge.net

## Finish up

* synchronize the *parent version* in `release.xml` and `update_libs.xml` to 
  match the new version in the top-level `pom.xml` file; this is required to 
  make the snapshots work ([jenkins](https://adams.cms.waikato.ac.nz/jenkins/job/MOA/), 
  [download](https://adams.cms.waikato.ac.nz/snapshots/moa/));
 
* ensure that the `Y` in `X.Y.Z` is two digits in all `pom.xml` files, `release.xml` 
  and `update_libs.xml`; the automatic increment will strip a leading zero.
  
* commit/push all changes

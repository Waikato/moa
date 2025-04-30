# Release instructions

## Prerequisites

* use Java 11+ for making release (Java 9 has outdated certificates). 
  Make sure the JAVA_HOME environment variable is set to the correct 
  JDK directory and exported in your terminal
  
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

* Ensure your GPG signing key and Sonatype Central access token (`server` with ID `central`) 
  are available in your Maven settings.xml file (found at either ~/.m2/ or /usr/share/maven/conf/).
  You can check the settings have been applied correctly by running the command:
  
   ```
   mvn help:effective-settings
   ```
   ```xml
  <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 http://maven.apache.org/xsd/settings-1.2.0.xsd">
    <localRepository>~/.m2/repository</localRepository>
    <servers>
      <server>
        <id>central</id>
        <username>XXXXXX</username>
        <password>***</password>
      </server>
    </servers>
    <profiles>
      <profile>
        <properties>
          <gpg.keyname>gpg_key_name</gpg.keyname>
          <gpg.passphrase></gpg.passphrase>
        </properties>
        <id>gpg</id>
      </profile>
    </profiles>
    <activeProfiles>
      <activeProfile>gpg</activeProfile>
    </activeProfiles>
    <pluginGroups>
      <pluginGroup>org.apache.maven.plugins</pluginGroup>
      <pluginGroup>org.codehaus.mojo</pluginGroup>
    </pluginGroups>
  </settings>
   ```
  The output should include your ``gpg_key_name`` and ``central`` credentials. gpg should be an active profile.

* For more information, follow these instructions:

  https://blog.sonatype.com/2010/01/how-to-generate-pgp-signatures-with-maven/

* You can generate your access token for publishing artifacts on Sonatype Central here:

  https://central.sonatype.com/account
  
* Publish your public GPG signing key to the keyserver at keyserver.ubuntu.com

* Run the following maven command (from the top-level MOA directory), which
  will automatically use the current version present in the `pom.xml` files
  and then increment it after the release has succeeded.

    ```
    mvn --batch-mode release:prepare release:perform
    ```

* Log into [https://central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)

* If deployment was successful, click on **Publish** otherwise on **Drop**.

* Perform a `git push`

* Check the following URL after 15-20min (sync with Maven Central only happens 
  every 15min or so) to see whether they artifacts are indeed available from 
  Maven Central (the search index at https://central.sonatype.com/ only gets updated
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
* Upload moa-release-XXXX.YY.Z-bin.zip to [MOA package repository on Sourceforge.net](https://sourceforge.net/projects/moa-datastream/)
* Update [MOA downloads page](https://moa.cms.waikato.ac.nz/downloads/) with the new link and latest Maven integration settings
* Create a new post about the latest release (use the old release template)
  ```
  New Release of MOA XX.YY
  
  We’ve made a new release of MOA XX.YY
  
  The new features of this release are:
    :
    :
  You can find the download link for this release on the MOA homepage (link to home page).
  
  Cheers,
  
  The MOA Team
  
  ```

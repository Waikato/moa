# Release instructions

## Prerequisites

* use Java 9+ for making release

* ensure that all `pom.xml` files list the same version, update them if 
  necessary to `yyyy.mm.0-SNAPSHOT` (check correct patch level)
  
* in the `weka-package/Description.props` file, ensure that the following
  properties have been updated and aligned with the upcoming version:
  
    * `Version` (yyyy.mm.0, check correct patch level)
    * `Date`
    * `PackageURL`

* ensure that all code has been committed/pushed


## Deploy to Maven Central

* Run the following maven command, which will automatically use the current
  version present in the `pom.xml` files and then increment it after the 
  release has succeeded. 

    ```
    mvn --batch-mode release:prepare release:perform
    ```

* Log into [https://oss.sonatype.org](https://oss.sonatype.org)

* Select **Staging Repositories**

* Subsequently **Close** and then **Release** the artifacts


## Generate release files

### MOA

* update the *parent version* in `release.xml` to the just released version,
  i.e., `yyyy.mm.0` (without the `-SNAPSHOT` suffix, check correct patch level)

* execute the following command (top-level directory)

    ```
    mvn -f release.xml clean package
    ```
    
### Weka Package    
    
* go into the `weka-package` directory 

* execute the following command to update libraries in the `lib` directory

   ```
   mvn -f update_libs.xml clean package
   ```

* commit/push the changes in regards to the libraries

* execute the following command (replace `X.Y.Z` with actual version of MOA, 
  eg `yyyy.mm.0`, check correct patch level)

    ```
    ant -f build_package.xml -Dpackage=massiveOnlineAnalysis-X.Y.Z clean make_package
    ```

## Publish release
    
* create new release tag on Gihub and upload the generated MOA release zip file 
  from the top-level `target` directory and the zip file from the 
  `weka-package/target` directory
  
* email Mark Hall (mhall at waikato.ac.nz) the link to the Weka package zip
  file to upload to the central Weka package repository on Sourceforge.net

* synchronize the *parent version* in `release.xml` and `update_libs.xml` to 
  match the new version in the top-level `pom.xml` file; this is required to 
  make the snapshots work ([jenkins](https://adams.cms.waikato.ac.nz/jenkins/job/MOA/), 
  [download](https://adams.cms.waikato.ac.nz/snapshots/moa/))
  
* commit/push all changes

# Release instructions

## Prerequisites

* ensure that all `pom.xml` files list the same version, update them if 
  necessary to `yyyy.m-SNAPSHOT`
  
* in the `weka-package/Description.props` file, ensure that the following
  properties have been updated and aligned with the upcoming version:
  
    * `Version` (yyyy.m.0)
    * `Date`
    * `PackageURL` (**TODO** github or sf.net?)

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

* update the *parent version* in `release.xml` to the just released version,
  i.e., `yyyy.m` (without the `-SNAPSHOT` suffix)

* execute the following command (top-level directory)

    ```
    mvn -f release.xml clean package
    ```
    
* go into the `weka-package` directory and execute the following command
  (replace `yyyy.m` with the actual release version)

    ```
    ant -f build_package.xml -Dpackage=massiveOnlineAnalysis-yyyy.m.0 clean make_package
    ```
    
* create new release tag on Gihub and upload the generated MOA release zip file 
  from the top-level `target` directory and the Weka package zip file from the
  `weka-package/target` directory

* synchronize the *parent version* in `release.xml` to match the new version
  in the top-level `pom.xml` file; this is required to make the snapshots work 
  ([jenkins](https://adams.cms.waikato.ac.nz/jenkins/job/MOA/), 
  [download](https://adams.cms.waikato.ac.nz/snapshots/moa/))
  
* commit/push all changes

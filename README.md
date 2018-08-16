Service Discovery Cleaner
------------------------------------

I made this tool to test functionality in Micronaut which builds a lot of these up, and there is no AWS UI to help clean this stuff up.
You can use this to recursively clean out a namespace and related services and instances, or wipe out all of them.


Run the parameter -h for help on command line parameters or take a peek at the build.gradle file to see examples.

If you do not use the -k and -s parameters, the default credentials provider chain will be used (i.e. in $HOME/.aws/credentials), env vars, etc.

Be very careful with the -a parameter, it will wipe out ALL namespaces in your service discovery space for your region!!!

Also use '-d' for dry run the first time you run it to verify what will be deleted!

---

```
usage: SDCleaner.groovy -[hndkrsca]
 -a,--delete-all-namespaces     ** delete ALL namespaces ** 
 -c,--recursive                 recursively remove
 -d,--dry-run                   show what will be deleted but make no
                                changes
 -h,--help                      Show usage information
 -k,--aws-key <aws-key>         aws-key to use for access
 -n,--namespace <namespace>     namespace id to clean
 -r,--region <region>           aws region to operate on
 -s,--aws-secret <aws-secret>   aws secret to use for access
```
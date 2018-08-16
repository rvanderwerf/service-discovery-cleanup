import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSSessionCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.AWSServiceDiscoveryClientBuilder
import com.amazonaws.services.servicediscovery.model.DeleteNamespaceRequest
import com.amazonaws.services.servicediscovery.model.DeleteNamespaceResult
import com.amazonaws.services.servicediscovery.model.DeleteServiceRequest
import com.amazonaws.services.servicediscovery.model.DeleteServiceResult
import com.amazonaws.services.servicediscovery.model.DeregisterInstanceRequest
import com.amazonaws.services.servicediscovery.model.GetNamespaceRequest
import com.amazonaws.services.servicediscovery.model.GetNamespaceResult
import com.amazonaws.services.servicediscovery.model.GetOperationRequest
import com.amazonaws.services.servicediscovery.model.GetOperationResult
import com.amazonaws.services.servicediscovery.model.InstanceSummary
import com.amazonaws.services.servicediscovery.model.ListInstancesRequest
import com.amazonaws.services.servicediscovery.model.ListInstancesResult
import com.amazonaws.services.servicediscovery.model.ListNamespacesRequest
import com.amazonaws.services.servicediscovery.model.ListNamespacesResult
import com.amazonaws.services.servicediscovery.model.ListServicesRequest
import com.amazonaws.services.servicediscovery.model.ListServicesResult
import com.amazonaws.services.servicediscovery.model.NamespaceSummary
import com.amazonaws.services.servicediscovery.model.ServiceFilter
import com.amazonaws.services.servicediscovery.model.ServiceSummary;
import org.apache.commons.cli.ParseException

class SDCleaner {


    public static void main(String[] args) {
        def cli = new CliBuilder(usage: 'SDCleaner.groovy -[hndkrsca]')
        // Create the list of options.
        cli.with {
            h longOpt: 'help', 'Show usage information'
            n longOpt: 'namespace', args: 1, argName: 'namespace', 'namespace id to clean'
            d longOpt: 'dry-run', 'show what will be deleted but make no changes'
            k longOpt: 'aws-key', args: 1, argName: 'aws-key', 'aws-key to use for access'
            r longOpt: 'region', args: 1, argName: 'region', 'aws region to operate on'
            s longOpt: 'aws-secret', args: 1, argName: 'aws-secret', 'aws secret to use for access'
            c longOpt: 'recursive', 'recursively remove'
            a longOpt: 'delete-all-namespaces', '** delete ALL namespaces ** '
        }

        OptionAccessor options = cli.parse(args)
        if (!options) {
            cli.usage()
            return
        }
        // Show usage text when -h or --help option is used.
        if (options.h) {
            cli.usage()
            return
        }

        AWSServiceDiscovery discoveryClient = null

        if (options.k && options.s) {
            BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials((options.k as String).trim(), (options.s as String).trim())
            AWSStaticCredentialsProvider provider = new AWSStaticCredentialsProvider(basicAWSCredentials)
            discoveryClient = AWSServiceDiscoveryClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(provider).build()
        } else {
            discoveryClient = AWSServiceDiscoveryClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(DefaultAWSCredentialsProviderChain.instance).build()
        }



        //ListNamespacesResult listNamespacesResult = discoveryClient.listNamespaces(new ListNamespacesRequest())

        def namespacesToDelete = []
        if (options.n) {
            namespacesToDelete.add(options.n.trim())
        }
        if (options.a) {
            ListNamespacesResult listNamespacesResult = discoveryClient.listNamespaces(new ListNamespacesRequest())
            listNamespacesResult.namespaces.each { NamespaceSummary namespaceSummary ->
                namespacesToDelete.add(namespaceSummary.id)
            }
        }

        namespacesToDelete.each { String namespaceId ->
            GetNamespaceRequest namespaceRequest = new GetNamespaceRequest().withId(namespaceId)

            GetNamespaceResult namespaceResult = discoveryClient.getNamespace(namespaceRequest)
            if (namespaceResult.namespace.serviceCount == 0) {
                if (!options.d) {
                    println("Deleting service ${namespaceId}")
                    DeleteNamespaceRequest deleteNamespaceRequest = new DeleteNamespaceRequest().withId(namespaceId);
                    discoveryClient.deleteNamespace(deleteNamespaceRequest);
                } else {
                    println("Would be deleting service ${namespaceId}")
                }
            } else {
                if (options.c) {
                    // get a list of services
                    ServiceFilter serviceFilter = new ServiceFilter().withName("NAMESPACE_ID").withValues(namespaceId)
                    ListServicesResult listServicesResult = discoveryClient.listServices(new ListServicesRequest().withFilters(serviceFilter))
                    listServicesResult.services.each { ServiceSummary serviceSummary ->
                        if (serviceSummary?.instanceCount == 0) {
                            if (!options.d && (options.c)) {
                                println("Deleting service id ${serviceSummary.id}")
                                DeleteServiceResult deleteServiceResult = discoveryClient.deleteService(new DeleteServiceRequest().withId(serviceSummary.id))

                            } else {
                                println("Would delete service id ${serviceSummary.id}")
                            }
                        } else {
                            if (options.c) {
                                ListInstancesResult listInstancesResult = discoveryClient.listInstances(new ListInstancesRequest().withServiceId(serviceSummary.id))

                                listInstancesResult.instances.each { InstanceSummary instanceSummary ->
                                    if (!options.d) {
                                        println("Deregistering instance id ${instanceSummary.id}")
                                        discoveryClient.deregisterInstance(new DeregisterInstanceRequest().withInstanceId(instanceSummary.id).withServiceId(serviceSummary.id))
                                    } else {
                                        println("Would deregister instance id ${instanceSummary.id} ")
                                    }
                                }
                            }
                        }
                        if (!options.d && options.c) {
                            // make sure it has no more instances
                            ListInstancesResult lastInstances = discoveryClient.listInstances(new ListInstancesRequest().withServiceId(serviceSummary.id))
                            if (lastInstances.instances.size() == 0) {
                                println("Deleting service id ${serviceSummary.id}")
                                DeleteServiceResult deleteServiceResult = discoveryClient.deleteService(new DeleteServiceRequest().withId(serviceSummary.id))
                            } else {
                                sleep(3000)
                                println("Deleting service id ${serviceSummary.id}")
                                DeleteServiceResult deleteServiceResult = discoveryClient.deleteService(new DeleteServiceRequest().withId(serviceSummary.id))
                                //println("Cannot delete service ${serviceSummary.id} because instances ${lastInstances.instances.toListString()} still exist!")
                            }
                        } else {
                            println("Would delete service id ${serviceSummary.id}")
                        }
                    }
                }
            }

            if (!options.d) {
                println("Deleting namespace id ${namespaceId}")
                DeleteNamespaceResult deleteNamespaceResult = discoveryClient.deleteNamespace(new DeleteNamespaceRequest().withId(namespaceId))
                GetOperationResult operationResult = discoveryClient.getOperation(new GetOperationRequest().withOperationId(deleteNamespaceResult.operationId))
            } else {
                println("Would delete namespace id ${namespaceId}")
            }
        }


    }
}





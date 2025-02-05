import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class LoadBalancing {
    List<Cloudlet> cloudletList;

    public static void main(String[] args) {
        Log.printLine("Starting LoadBalancing...");

        try {
            int num_user = 3; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            // Create a Datacenter
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            // Create a DatacenterBroker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Create VMs
            List<Vm> vmList = createVms(brokerId);

            // Submit VMs to the broker
            broker.submitVmList(vmList);

            // Create Cloudlets
            List<Cloudlet> cloudletList = createCloudlets(brokerId);

            // Submit Cloudlets to the broker
            broker.submitCloudletList(cloudletList);

            // Start the simulation
            CloudSim.startSimulation();

            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
            printCloudletList(finishedCloudlets);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Datacenter createDatacenter(String name) {
        // Create a list to store hosts
        List<Host> hostList = new ArrayList<Host>();

        // Define the characteristics of the hosts
        int mips = 1000;
        int ram = 2048; // host memory (MB)
        long storage = 1000000; // host storage in MB (1 TB)
        int bw = 10000; // bandwidth (Mbps)

        // Create PEs (Processing Elements) for the host
        List<Pe> peList = new ArrayList<Pe>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        // Create 2 hosts with the specified PEs, RAM, storage, and bandwidth
        for (int hostId = 0; hostId < 3; hostId++) {
            hostList.add(
                    new Host(
                            hostId,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(bw),
                            storage,
                            peList,
                            new VmSchedulerTimeShared(peList))
            );
        }

        // Create a Datacenter with the host list
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen"; // virtual machine monitor
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this resource
        double costPerBw = 0.0; // the cost of using bandwidth in this resource

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
                cost, costPerMem, costPerStorage, costPerBw);

        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<Storage>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static DatacenterBroker createBroker() {
        try {
            return new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Vm> createVms(int brokerId) {
        List<Vm> vmlist = new ArrayList<Vm>();

        // Define the characteristics of the VM
        int mips = 1000;
        int pesNumber = 1; // number of CPUs
        int ram = 512; // VM memory (MB)
        long bw = 1000; // bandwidth (Mbps)
        long size = 10000; // image size storage (MB)
        String vmm = "Xen"; // virtual machine monitor

        // Create 4 VMs
        for (int vmId = 0; vmId < 4; vmId++) {
            vmlist.add(
                    new Vm(
                            vmId,
                            brokerId,
                            mips,
                            pesNumber,
                            ram,
                            bw,
                            size,
                            vmm,
                            new CloudletSchedulerTimeShared()
                    )
            );
        }
        return vmlist;
    }

    public static List<Cloudlet> createCloudlets(int brokerId) {
        List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();

        // Define the characteristics of the cloudlets
        long length = 4000; // MI (Million Instructions)
        long fileSize = 300; // input file size (MB)
        long outputSize = 300; // output file size (MB)
        int pesNumber = 1; // number of CPUs
        UtilizationModel utilizationModel = new UtilizationModelFull();

        // Create 10 Cloudlets
        for (int cloudletId = 0; cloudletId < 10; cloudletId++) {
            Cloudlet cloudlet = new Cloudlet(
                    cloudletId,
                    length,
                    pesNumber,
                    fileSize,
                    outputSize,
                    utilizationModel,   // CPU
                    utilizationModel,   // RAM
                    utilizationModel    // Bandwidth
            );
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }
        return cloudletList;
    }

    public static void printCloudletList(List<Cloudlet> cloudletList) {
        System.out.println("========== Cloudlet Execution Results ==========");
        System.out.println("Cloudlet ID\tStatus\tDataCenter ID\tVM ID\tTime\tStart Time\tFinish Time");

        for (Cloudlet cloudlet : cloudletList) {
            System.out.println(
                    cloudlet.getCloudletId() + "\t\t" +
                    (cloudlet.getStatus() == Cloudlet.SUCCESS ? "SUCCESS" : "FAILED") + "\t\t" +
                    cloudlet.getResourceId() + "\t\t" + // Datacenter ID
                    cloudlet.getVmId() + "\t\t" +
                    cloudlet.getActualCPUTime() + "\t\t" +
                    cloudlet.getExecStartTime() + "\t\t" +
                    cloudlet.getFinishTime()
            );
        }
    }

}
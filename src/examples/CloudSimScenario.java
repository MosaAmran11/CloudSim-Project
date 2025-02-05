package examples;
/*السيناريو:
نريد محاكاة مركز بيانات صغير يقوم بإدارة مجموعة من الآلات الافتراضية (VMs) لتشغيل مجموعة من المهام (Cloudlets). يحتوي هذا المركز على مضيف واحد فقط (Host) مع الموارد التالية:

المضيف (Host):

وحدة معالجة مركزية بسرعة 1000 MIPS.
ذاكرة RAM بسعة 2GB.
مساحة تخزين بسعة 1TB.
عرض نطاق ترددي 10,000.
الآلات الافتراضية (VMs):

آلة افتراضية واحدة بمواصفات:
سرعة معالجة: 1000 MIPS.
ذاكرة RAM: 512MB.
مساحة تخزين: 10GB.
عرض نطاق ترددي: 1000.
المهام (Cloudlets):

3 مهام، كل واحدة لديها 40,000 تعليمات تحتاج إلى المعالجة.
كل مهمة لديها حجم ملف إدخال وإخراج 300KB.
الهدف: نريد أن نرى كيف تتم جدولة وتنفيذ هذه المهام على الآلة الافتراضية باستخدام مكتبة CloudSim.*/
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class CloudSimScenario {
    public static void main(String[] args) {
        try {
            // 1. Initialize the CloudSim library
            int numUsers = 1; // عدد المستخدمين
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUsers, calendar, false);

            // 2. Create a Datacenter
            Datacenter datacenter = createDatacenter("Datacenter_0");

            // 3. Create a Broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // 4. Create Virtual Machines (VMs)
            List<Vm> vmList = new ArrayList<>();
            int vmId = 0;
            int mips = 1000;
            long size = 10000; // حجم القرص (بالـ MB)
            int ram = 512; // RAM (بالـ MB)
            long bw = 1000; // عرض النطاق الترددي
            int pesNumber = 1; // عدد المعالجات
            String vmm = "Xen"; // نوع مدير الأجهزة الافتراضية

            Vm vm = new Vm(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vmList.add(vm);

            // إرسال الآلة الافتراضية إلى الوسيط
            broker.submitVmList(vmList);

            // 5. Create Cloudlets (Tasks)
            List<Cloudlet> cloudletList = new ArrayList<>();
            int cloudletId = 0;
            long length = 40000; // طول المهمة (عدد التعليمات)
            long fileSize = 300; // حجم الملف (بالـ KB)
            long outputSize = 300; // حجم الإخراج (بالـ KB)
            UtilizationModel utilizationModel = new UtilizationModelFull();

            for (int i = 0; i < 3; i++) { // إنشاء 3 مهام
                Cloudlet cloudlet = new Cloudlet(
                        cloudletId++, length, pesNumber, fileSize, outputSize,
                        utilizationModel, utilizationModel, utilizationModel
                );
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            // إرسال المهام إلى الوسيط
            broker.submitCloudletList(cloudletList);

            // 6. Start the simulation
            CloudSim.startSimulation();

            // 7. Print results
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            printCloudletList(newList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        // Create a list of hosts
        List<Host> hostList = new ArrayList<>();

        // Create a list of processing elements (PEs)
        List<Pe> peList = new ArrayList<>();
        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // معالج واحد بسرعة 1000 MIPS

        // Create Host with its specifications
        int hostId = 0;
        int ram = 2048; // RAM (بالـ MB)
        long storage = 1000000; // Storage (بالـ MB)
        int bw = 10000; // Bandwidth

        Host host = new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList)
        );

        hostList.add(host);

        // Create a DatacenterCharacteristics object
        String arch = "x86"; // Architecture
        String os = "Linux"; // Operating system
        String vmm = "Xen"; // Virtual Machine Monitor
        double timeZone = 10.0; // Time zone this resource is located
        double costPerSec = 3.0; // Cost per second
        double costPerMem = 0.05; // Cost per MB
        double costPerStorage = 0.001; // Cost per MB
        double costPerBw = 0.0; // Cost per bandwidth

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, timeZone, costPerSec, costPerMem, costPerStorage, costPerBw);

        // Finally, create a Datacenter object.
        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new ArrayList<>(), 0);
    }

    private static DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("Broker");
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        for (Cloudlet cloudlet : list) {
            System.out.print(indent + cloudlet.getCloudletId() + indent);

            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                System.out.println("SUCCESS"
                        + indent + cloudlet.getResourceId()
                        + indent + cloudlet.getVmId()
                        + indent + cloudlet.getActualCPUTime()
                        + indent + cloudlet.getExecStartTime()
                        + indent + cloudlet.getFinishTime());
            }
        }
    }
}

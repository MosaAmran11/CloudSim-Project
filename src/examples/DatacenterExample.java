package examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class DatacenterExample {

    public static void main(String[] args) {
        // 1. تهيئة المحاكي
        int numUsers = 1; // عدد المستخدمين السحابيين
        Calendar calendar = Calendar.getInstance();
        boolean traceFlag = false; // تعطيل تتبع الأحداث
        CloudSim.init(numUsers, calendar, traceFlag);

        // 2. إنشاء مركز بيانات (Datacenter)
        Datacenter datacenter = createDatacenter("Datacenter_0");

        // 3. إنشاء وسيط المستخدم (DatacenterBroker)
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4. إنشاء قائمة الأجهزة الافتراضية (VMs)
        List<Vm> vmList = createVMs(broker.getId(), 5); // إنشاء 5 VMs

        // 5. إرسال قائمة VMs إلى الوسيط
        assert broker != null;
        broker.submitVmList(vmList);

        // 6. تشغيل المحاكي
        CloudSim.startSimulation();

        // 7. إنهاء المحاكاة
        CloudSim.stopSimulation();

        // 8. عرض النتائج
        List<Cloudlet> cloudletReceivedList = broker.getCloudletReceivedList();
        for (Cloudlet cloudlet : cloudletReceivedList) {
            System.out.println("Cloudlet ID: " + cloudlet.getCloudletId() + " executed on VM ID: " + cloudlet.getVmId());
        }
    }

    /**
     * دالة لإنشاء مركز بيانات يحتوي على مجموعة من المضيفين (Hosts)
     */
    private static Datacenter createDatacenter(String name) {
        // قائمة المضيفين
        List<Host> hostList = new ArrayList<>();

        // 1. إعداد الموارد لكل مضيف
        int mips = 1000;
        int ram = 16384; // RAM بحجم 16GB
        long storage = 1000000; // تخزين 1TB
        int bw = 10000; // عرض النطاق الترددي 10GB

        // 2. إعداد قائمة وحدات المعالجة (PEs)
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // وحدة معالجة واحدة لكل مضيف

        // 3. إنشاء مضيف وإضافته إلى القائمة
        hostList.add(new Host(0, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw),
                storage, peList, new VmSchedulerTimeShared(peList)));

        // 4. إعداد خصائص مركز البيانات
        String arch = "x86"; // architecture
        String os = "Linux"; // operating system
        String vmm = "Xen"; // Virtual Machine Monitor
        double time_zone = 10.0; // التوقيت المحلي
        double costPerSec = 3.0; // تكلفة الحساب لكل ثانية
        double costPerMem = 0.05; // تكلفة الذاكرة لكل MB
        double costPerStorage = 0.001; // تكلفة التخزين لكل GB
        double costPerBw = 0.0; // تكلفة عرض النطاق الترددي

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, costPerSec, costPerMem, costPerStorage, costPerBw);

        // 5. إنشاء مركز البيانات
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new ArrayList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    /**
     * دالة لإنشاء قائمة من الأجهزة الافتراضية (VMs)
     */
    private static List<Vm> createVMs(int brokerId, int numVMs) {
        List<Vm> vmList = new ArrayList<>();
        int mips = 500; // القدرة الحسابية لكل VM
        int ram = 2048; // RAM لكل VM (2GB)
        long bw = 1000; // عرض النطاق الترددي (1GB)
        long size = 10000; // حجم التخزين (10GB)
        String vmm = "Xen"; // Virtual Machine Monitor

        for (int i = 0; i < numVMs; i++) {
            Vm vm = new Vm(i, brokerId, mips, 1, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }

        return vmList;
    }
}

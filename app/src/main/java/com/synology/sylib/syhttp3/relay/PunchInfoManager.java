package com.synology.sylib.syhttp3.relay;

import com.synology.synoholepunch.PunchInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/* loaded from: classes2.dex */
public class PunchInfoManager {
    private static PunchInfoManager sInstance;
    private Map<DaemonInfo, List<PunchInfo>> mPunchInfoMap = new HashMap();

    public static PunchInfoManager getInstance() {
        if (sInstance == null) {
            synchronized (PunchInfoManager.class) {
                if (sInstance == null) {
                    sInstance = new PunchInfoManager();
                }
            }
        }
        return sInstance;
    }

    public synchronized void add(String str, String str2, PunchInfo punchInfo) {
        List<PunchInfo> arrayList = this.mPunchInfoMap.get(getKey(str, str2));
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mPunchInfoMap.put(getKey(str, str2), arrayList);
        }
        Iterator<PunchInfo> it = arrayList.iterator();
        while (it.hasNext()) {
            if (it.next().getTcpPort() == punchInfo.getTcpPort()) {
                return;
            }
        }
        arrayList.add(punchInfo);
    }

    public synchronized void remove(String str, String str2) {
        List<PunchInfo> list = get(str, str2);
        if (list == null) {
            return;
        }
        for (PunchInfo punchInfo : list) {
            if (punchInfo != null) {
                punchInfo.delete();
            }
        }
        this.mPunchInfoMap.remove(getKey(str, str2));
    }

    public synchronized void remove(String str, String str2, int i) {
        List<PunchInfo> list = get(str, str2);
        if (list == null) {
            return;
        }
        Iterator<PunchInfo> it = list.iterator();
        while (it.hasNext()) {
            PunchInfo next = it.next();
            if (next != null && next.getTcpPort() == i) {
                next.delete();
                it.remove();
            }
        }
    }

    public synchronized void stop(String str, String str2) {
        List<PunchInfo> list = get(str, str2);
        if (list == null) {
            return;
        }
        for (PunchInfo punchInfo : list) {
            if (punchInfo != null) {
                punchInfo.stop();
            }
        }
    }

    public synchronized void stop(String str, String str2, int i) {
        List<PunchInfo> list = get(str, str2);
        if (list == null) {
            return;
        }
        for (PunchInfo punchInfo : list) {
            if (punchInfo != null && punchInfo.getTcpPort() == i) {
                punchInfo.stop();
            }
        }
    }

    public synchronized void stopAll() {
        for (List<PunchInfo> list : this.mPunchInfoMap.values()) {
            if (list != null) {
                for (PunchInfo punchInfo : list) {
                    if (punchInfo != null) {
                        punchInfo.stop();
                    }
                }
            }
        }
    }

    public synchronized List<PunchInfo> get(String str, String str2) {
        return this.mPunchInfoMap.get(getKey(str, str2));
    }

    public synchronized PunchInfo getLastestPunchInfo(String str, String str2) {
        List<PunchInfo> list = get(str, str2);
        if (list != null && list.size() != 0) {
            return list.get(list.size() - 1);
        }
        return null;
    }

    public synchronized DaemonInfo getDaemonInfo(int i) {
        for (DaemonInfo daemonInfo : this.mPunchInfoMap.keySet()) {
            for (PunchInfo punchInfo : this.mPunchInfoMap.get(daemonInfo)) {
                if (punchInfo != null && punchInfo.getTcpPort() == i) {
                    return daemonInfo;
                }
            }
        }
        return null;
    }

    private DaemonInfo getKey(String str, String str2) {
        return new DaemonInfo(str, str2);
    }

    public static class DaemonInfo {
        String serverID;
        String serviceID;

        public DaemonInfo(String str, String str2) {
            this.serverID = str;
            this.serviceID = str2;
        }

        public String getServerID() {
            return this.serverID;
        }

        public String getServiceID() {
            return this.serviceID;
        }

        public int hashCode() {
            return new HashCodeBuilder(17, 31).append(this.serverID).append(this.serviceID).toHashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof DaemonInfo)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            DaemonInfo daemonInfo = (DaemonInfo) obj;
            return new EqualsBuilder().append(this.serverID, daemonInfo.serverID).append(this.serviceID, daemonInfo.serviceID).isEquals();
        }
    }
}
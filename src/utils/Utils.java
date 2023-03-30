package utils;

import messages.Emergency;

import java.util.List;

public class Utils {
    public static void RemoveEmergency(List<Emergency> list, String toRemove) {
        boolean found = false;
        int emToRemove = -1;
        for (int i = 0; i < list.size() && !found; i++) {
            if (list.get(i).getMessage().equals(toRemove)) {
                emToRemove = i;
                found = true;
            }
        }
        if (emToRemove != -1) list.remove(emToRemove);
    }

    public static Emergency findEmergencyByName(List<Emergency> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getMessage().equals(name)) {
                return list.get(i);
            }
        }
        return null;
    }
}

package com.farex.ui.operator;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.farex.Api;

import farex.shared.fare.DiscountedFare;
import farex.shared.fare.FareRule;
import farex.shared.fare.Fares;
import farex.shared.fare.RegularFare;

// The 12 fares from the 16 Nov shift that were never synced (fake data, like MockData).
// They are the "12 records not yet synced" on the Dashboard and the older part of History.
public final class SampleShift {

    static final String DATE = "16 Nov";

    private SampleShift() {
    }

    // Newest first, like History. Fares use the real farex-shared formula on the route's stops.
    static List<ShiftRecord> records(List<Api.StopCoord> stops) {
        List<ShiftRecord> list = new ArrayList<>();
        //            time      card?  who          from to student maxFare balance after
        list.add(fare(stops, "18:24", true, "**** 6767", 0, 2, true, false, 512.40));
        list.add(fare(stops, "18:21", true, "**** 9124", 0, 5, false, true, 84.95));
        list.add(fare(stops, "18:18", false, "APP-0821", 2, 5, false, false, null));
        list.add(fare(stops, "18:02", true, "**** 1111", 1, 3, false, false, 118.20));
        list.add(fare(stops, "17:47", true, "**** 4410", 0, 4, true, false, 230.66));
        list.add(fare(stops, "17:31", false, "APP-0632", 3, 5, true, false, null));
        list.add(fare(stops, "17:15", true, "**** 6767", 0, 1, false, false, 543.21));
        list.add(fare(stops, "16:58", true, "**** 2087", 2, 4, false, false, 61.70));
        list.add(fare(stops, "16:40", false, "APP-0915", 0, 3, false, false, null));
        list.add(fare(stops, "16:22", true, "**** 9124", 1, 5, false, false, 125.00));
        list.add(fare(stops, "16:05", true, "**** 3356", 0, 2, true, false, 300.34));
        list.add(fare(stops, "15:49", true, "**** 1111", 3, 4, false, false, 148.02));
        return list;
    }

    // App fares (balance null) were saved offline, so their payment is still pending
    private static ShiftRecord fare(List<Api.StopCoord> stops, String time, boolean card, String who,
                                    int from, int to, boolean student, boolean maxFare, Double balance) {
        double km = 0;
        for (int i = from; i < to; i++) {
            Api.StopCoord p = stops.get(i);
            Api.StopCoord q = stops.get(i + 1);
            km += Fares.haversineKm(p.lat(), p.lng(), q.lat(), q.lng());
        }
        FareRule rule = student ? new DiscountedFare() : new RegularFare();
        long cents = Fares.fare(km, rule);
        long base = Fares.fare(km, new RegularFare());
        String toName = maxFare ? "Route end" : stops.get(to).name();
        return new ShiftRecord(LocalTime.parse(time), !card, who, stops.get(from).name(), toName, km,
                cents, base, balance, maxFare, !card);
    }
}

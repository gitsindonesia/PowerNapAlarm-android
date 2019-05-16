package com.gits.powernap.db;

public class NapTheory {
    public static NapTime[] listNapTime = {
            new NapTime("The Nano-Nap", 20 * 1000, "The Nano-Nap\n\nSleep studies haven't yet concluded whether there are benefits to these brief intervals, like when you nod off on someone's shoulder on the train."),
            new NapTime("The Micro-Nap", 5 * 60 * 1000, "The Micro-Nap\n\nShown to be surprisingly effective at shedding sleepiness."),
            new NapTime("The Mini-Nap", 15 * 60 * 1000, "The Mini-Nap\n\nIncreases alertness, stamina, motor learning, and motor performance."),
            new NapTime("The Original Power Nap", 20 * 60 * 1000, "The Original Power Nap\n\nIncludes the benefits of the micro and the mini, but additionally improves muscle memory and clears the brain of useless built-up information, which helps with long-term memory (remembering facts, events, and names)."),
            new NapTime("The Maximum Nap", 30 * 60 * 1000, "The Maximum Nap\n\nWhen taking a nap longer than 30 minutes, you run the risk of heading into deep sleep, which will leave you feeling tired and groggy."),
            new NapTime("The Lazy Man's Nap", 60 * 60 * 1000, "The Lazy Man's Nap\n\nIncludes slow-wave plus REM sleep; good for improving perceptual processing; also when the system is flooded with human growth hormone, great for repairing bones and muscles.")
    };

    public static NapTime getNapByTime(long time) {
        for (int i = 0; i < listNapTime.length; i++) {
            if (listNapTime[i].getDuration() == time) {
                return listNapTime[i];
            }
        }
        return null;
    }

    public static String[] getTitles() {
        String[] res = new String[listNapTime.length];

        for (int i = 0; i < listNapTime.length; i++) {
            res[i] = listNapTime[i].label;
        }
        return res;
    }

    public static class NapTime {
        private String label;
        private long duration;
        private String description;

        public String getLabel() {
            return label;
        }

        public long getDuration() {
            return duration;
        }

        public String getDescription() {
            return description;
        }

        public NapTime(String label, long duration, String description) {
            this.label = label;
            this.duration = duration;
            this.description = description;
        }
    }
}

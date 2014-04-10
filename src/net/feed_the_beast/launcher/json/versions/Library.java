package net.feed_the_beast.launcher.json.versions;

import java.util.List;
import java.util.Map;

import net.ftb.download.Locations;

public class Library {
    public String name;
    public List<OSRule> rules;
    public Map<OS, String> natives;
    public ExtractRule extract;
    public String url;

    private Action _applies = null;

    public boolean applies () {
        if (_applies == null) {
            _applies = Action.DISALLOW;
            if (rules == null) {
                _applies = Action.ALLOW;
            } else {
                for (OSRule rule : rules) {
                    if (rule.applies())
                        _applies = rule.action;
                }
            }
        }
        return _applies == Action.ALLOW;
    }

    private Artifact _artifact = null;

    public String getPath () {
        if (_artifact == null) {
            _artifact = new Artifact(name);
        }
        return _artifact.getPath();
    }

    public String getPathNatives () {
        if (natives == null)
            return null;
        if (_artifact == null) {
            _artifact = new Artifact(name);
        }
        return _artifact.getPath(natives.get(OS.CURRENT));
    }

    public String getUrl () {
        return url == null ? Locations.mc_libs : url;
    }

    @Override
    public String toString () {
        return name;
    }

    private class Artifact {
        private String domain;
        private String name;
        private String version;
        private String classifier;
        private String ext = "jar";

        public Artifact(String rep) {
            String[] pts = rep.split(":");
            int idx = pts[pts.length - 1].indexOf('@');
            if (idx != -1) {
                ext = pts[pts.length - 1].substring(idx + 1);
                pts[pts.length - 1] = pts[pts.length - 1].substring(0, idx);
            }
            domain = pts[0];
            name = pts[1];
            version = pts[2];
            if (pts.length > 3)
                classifier = pts[3];
        }

        public String getPath () {
            return getPath(classifier);
        }

        public String getPath (String classifier) {
            String ret = String.format("%s/%s/%s/%s-%s", domain.replace('.', '/'), name, version, name, version);
            if (classifier != null)
                ret += "-" + classifier;
            return ret + "." + ext;
        }
    }
}

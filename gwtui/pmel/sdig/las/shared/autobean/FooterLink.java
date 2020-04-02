package pmel.sdig.las.shared.autobean;

public class FooterLink implements Comparable {
    String url;
    String text;
    int index;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int compareTo(Object o) {
        if ( o instanceof FooterLink ) {
            FooterLink fl = (FooterLink) o;
            return index - fl.index;
        }
        return 0;
    }
}

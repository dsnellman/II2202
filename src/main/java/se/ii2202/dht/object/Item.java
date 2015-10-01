package se.ii2202.dht.object;

public class Item {

    public int key;
    public int value;

    public Item(int key) {
        this.key = key;
        this.value = -1;
    }

    public Item(int key, int value) {
        this.key = key;
        this.value = value;
    }

    public String toString(){

        return String.valueOf(key);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + this.key;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Item other = (Item) obj;
        if (this.key != other.key) {
            return false;
        }
        return true;
    }

}

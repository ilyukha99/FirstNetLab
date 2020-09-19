package Net;

public class Pair<T, U> {
    private T first;
    private U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof Pair) {
            Pair<T, U> tmp = (Pair<T, U>)other;
            return this.first.equals(tmp.first) && this.second.equals(tmp.second);
        }

        return false;
    }

    public int hashCode() {
        return (first != null && second != null) ? first.hashCode() + second.hashCode() : 0;
    }

    public String toString() {
        return "[" + first.toString() + ", " + second.toString() + "]";
    }
}
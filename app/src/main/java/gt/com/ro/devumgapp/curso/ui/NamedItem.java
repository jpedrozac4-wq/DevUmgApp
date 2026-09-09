package gt.com.ro.devumgapp.curso.ui;

final class NamedItem<T> {

    final String label;
    final T value;

    NamedItem(String label, T value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public String toString() {
        return label;
    }
}

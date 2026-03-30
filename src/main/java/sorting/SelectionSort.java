package sorting;

import java.util.List;
import model.RegistroFinanciero;

//Implementamos la nueva interfaz
public class SelectionSort implements Sorter<RegistroFinanciero> {

    @Override
    public void sort(List<RegistroFinanciero> datos) {
        int n = datos.size();

        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;

            for (int j = i + 1; j < n; j++) {
                if (datos.get(j).compareTo(datos.get(minIdx)) < 0) {
                    minIdx = j;
                }
            }

            // Intercambio (Swap)
            if (minIdx != i) {
                RegistroFinanciero temp = datos.get(minIdx);
                datos.set(minIdx, datos.get(i));
                datos.set(i, temp);
            }
        }
    }

    @Override
    public String getNombre() {
        return "Selection Sort";
    }
}
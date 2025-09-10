package com.alexgls.springboot.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;



public class FilenameGenerator {

    public static final Map<Character, String> RUSSIAN_TO_ENGLISH = new HashMap<>();

    static {
        // Строчные буквы
        RUSSIAN_TO_ENGLISH.put('а', "a");
        RUSSIAN_TO_ENGLISH.put('б', "b");
        RUSSIAN_TO_ENGLISH.put('в', "v");
        RUSSIAN_TO_ENGLISH.put('г', "g");
        RUSSIAN_TO_ENGLISH.put('д', "d");
        RUSSIAN_TO_ENGLISH.put('е', "e");
        RUSSIAN_TO_ENGLISH.put('ё', "yo");
        RUSSIAN_TO_ENGLISH.put('ж', "zh");
        RUSSIAN_TO_ENGLISH.put('з', "z");
        RUSSIAN_TO_ENGLISH.put('и', "i");
        RUSSIAN_TO_ENGLISH.put('й', "y");
        RUSSIAN_TO_ENGLISH.put('к', "k");
        RUSSIAN_TO_ENGLISH.put('л', "l");
        RUSSIAN_TO_ENGLISH.put('м', "m");
        RUSSIAN_TO_ENGLISH.put('н', "n");
        RUSSIAN_TO_ENGLISH.put('о', "o");
        RUSSIAN_TO_ENGLISH.put('п', "p");
        RUSSIAN_TO_ENGLISH.put('р', "r");
        RUSSIAN_TO_ENGLISH.put('с', "s");
        RUSSIAN_TO_ENGLISH.put('т', "t");
        RUSSIAN_TO_ENGLISH.put('у', "u");
        RUSSIAN_TO_ENGLISH.put('ф', "f");
        RUSSIAN_TO_ENGLISH.put('х', "kh");
        RUSSIAN_TO_ENGLISH.put('ц', "ts");
        RUSSIAN_TO_ENGLISH.put('ч', "ch");
        RUSSIAN_TO_ENGLISH.put('ш', "sh");
        RUSSIAN_TO_ENGLISH.put('щ', "shch");
        RUSSIAN_TO_ENGLISH.put('ъ', "");
        RUSSIAN_TO_ENGLISH.put('ы', "y");
        RUSSIAN_TO_ENGLISH.put('ь', "");
        RUSSIAN_TO_ENGLISH.put('э', "e");
        RUSSIAN_TO_ENGLISH.put('ю', "yu");
        RUSSIAN_TO_ENGLISH.put('я', "ya");

        // Заглавные буквы
        RUSSIAN_TO_ENGLISH.put('А', "A");
        RUSSIAN_TO_ENGLISH.put('Б', "B");
        RUSSIAN_TO_ENGLISH.put('В', "V");
        RUSSIAN_TO_ENGLISH.put('Г', "G");
        RUSSIAN_TO_ENGLISH.put('Д', "D");
        RUSSIAN_TO_ENGLISH.put('Е', "E");
        RUSSIAN_TO_ENGLISH.put('Ё', "Yo");
        RUSSIAN_TO_ENGLISH.put('Ж', "Zh");
        RUSSIAN_TO_ENGLISH.put('З', "Z");
        RUSSIAN_TO_ENGLISH.put('И', "I");
        RUSSIAN_TO_ENGLISH.put('Й', "Y");
        RUSSIAN_TO_ENGLISH.put('К', "K");
        RUSSIAN_TO_ENGLISH.put('Л', "L");
        RUSSIAN_TO_ENGLISH.put('М', "M");
        RUSSIAN_TO_ENGLISH.put('Н', "N");
        RUSSIAN_TO_ENGLISH.put('О', "O");
        RUSSIAN_TO_ENGLISH.put('П', "P");
        RUSSIAN_TO_ENGLISH.put('Р', "R");
        RUSSIAN_TO_ENGLISH.put('С', "S");
        RUSSIAN_TO_ENGLISH.put('Т', "T");
        RUSSIAN_TO_ENGLISH.put('У', "U");
        RUSSIAN_TO_ENGLISH.put('Ф', "F");
        RUSSIAN_TO_ENGLISH.put('Х', "Kh");
        RUSSIAN_TO_ENGLISH.put('Ц', "Ts");
        RUSSIAN_TO_ENGLISH.put('Ч', "Ch");
        RUSSIAN_TO_ENGLISH.put('Ш', "Sh");
        RUSSIAN_TO_ENGLISH.put('Щ', "Shch");
        RUSSIAN_TO_ENGLISH.put('Ъ', "");
        RUSSIAN_TO_ENGLISH.put('Ы', "Y");
        RUSSIAN_TO_ENGLISH.put('Ь', "");
        RUSSIAN_TO_ENGLISH.put('Э', "E");
        RUSSIAN_TO_ENGLISH.put('Ю', "Yu");
        RUSSIAN_TO_ENGLISH.put('Я', "Ya");
    }

    // Метод для транслитерации всей строки
    public static String transliterate(String russianText) {
        if (russianText == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (char c : russianText.toCharArray()) {
            String englishEquivalent = RUSSIAN_TO_ENGLISH.get(c);
            if (englishEquivalent != null) {
                result.append(englishEquivalent);
            } else {
                // Если символ не русская буква, оставляем как есть
                result.append(c);
            }
        }
        return result.toString();
    }

    // Пример использования
    public static void main(String[] args) {
        String russian = "Привет мир! Это тест: Ёж, Шишка, Цветок";
        String english = transliterate(russian);
        System.out.println(english); // Privet mir! Eto test: Yozh, Shishka, Tsvetok
    }
}
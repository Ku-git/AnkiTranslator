package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplitExample {

    public static void main(String[] args) {

        String text = "<table><tbody><tr><td colspan=\"2\"><div>occurs when the diencephalon and midbrain are displaced caudally due to increased intracranial pressure (eg, intracranial hemorrhage).&nbsp;&nbsp;<strong><br><br><br>Clinical features of brain herniation</strong></div></td></tr><tr><td><div><strong>Herniation type</strong></div></td><td><div><strong>Common features</strong></div></td></tr><tr><td><div><strong>Subfalcine</strong></div></td><td><ul><li>Herniation of cingulate gyrus underneath falx cerebri</li><li>No pupillary involvement, consciousness often preserved</li><li>Contralateral leg weakness (ipsilateral ACA compression)</li></ul></td></tr><tr><td><div><strong>Uncal</strong></div></td><td><ul><li>Herniation of uncus (medial temporal lobe) under tentorium cerebelli</li><li>Ipsilateral dilated &amp; fixed pupil (ipsilateral oculomotor nerve [CN III] compression)</li><li>Early: contralateral hemiparesis (ipsilateral cerebral peduncle compression)</li><li>Late: ipsilateral hemiparesis (contralateral cerebral peduncle compression)</li></ul></td></tr><tr><td><div><strong>Central</strong></div></td><td><ul><li>Caudal displacement of diencephalon &amp; brainstem</li><li>Rupture of paramedian basilar artery branches</li><li>Bilateral midposition &amp; fixed pupils (loss of sympathetic &amp; parasympathetic innervation)</li><li>Decorticate (flexor) → decerebrate (extensor) posturing</li></ul></td></tr><tr><td><div><strong>Tonsillar</strong></div></td><td><ul><li>Herniation of cerebellar tonsils through foramen magnum</li><li>Coma, loss of CN reflexes, flaccid paralysis, respiratory arrest (brainstem compression)</li></ul></td></tr><tr><td colspan=\"2\"><div><strong>ACA</strong> = anterior cerebral artery; <strong>CN</strong> = cranial nerve.<br><br><img src=\"ICH-d715c83c7750e75afaafe1287086559429042dd4.png\"><br></div></td></tr></tbody></table>";

        List<String> spiltText = splitProcess(text, 135);
        spiltText.forEach(System.out::println);
    }

    private static List<String> splitProcess(String text, int maxLength) {

        List<String> result = new ArrayList<>();
        String[] sentences = text.split("(?<=\\.)\\s+(?!jpg)|(?=<[^>]+>)|(?<=</[^>]>)");
        StringBuilder currentPart = new StringBuilder();

        for (String sentence: sentences) {
            if (currentPart.length() + sentence.length() > maxLength) {
                result.add(currentPart.toString());
                currentPart = new StringBuilder();
            }
            if (sentence.length() > maxLength) {
                while (sentence.length() > maxLength) {

                    int splitIndex = findSplitIndex(sentence, maxLength);
                    result.add(sentence.substring(0, splitIndex).trim());
                    sentence = sentence.substring(splitIndex).trim(); // 剩余部分继续处理
                }
                result.add(sentence);
                continue;
            }

            currentPart.append(sentence);
        }

        if(!currentPart.isEmpty()) {
        result.add(currentPart.toString());
        }
        result.removeAll(Arrays.asList("", null));

        return result;
    }

    private static int findSplitIndex(String sentence, int maxLength) {

        int splitIndex = sentence.lastIndexOf(',', maxLength);
        if (splitIndex == -1 || splitIndex == 0) {
            splitIndex = maxLength; // 如果找不到合适的拆分点，就直接在128字符处拆分
        }
        return splitIndex;
    }

}

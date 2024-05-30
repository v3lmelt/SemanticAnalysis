package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductionConst {
    public static Map<Integer, Production> PRODUCTION_MAP = new HashMap<>();


    public static void getProductionFromFile(String filePath){
        try{
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while(bufferedReader.ready()){
                String content = bufferedReader.readLine();
                ArrayList<String> dividedLine = new ArrayList<>(List.of(content.split(" ")));

                if(dividedLine.size() < 4) throw new Exception("Invalid input of productions: +" + content);
                ArrayList<String> rightPart = new ArrayList<>();
                for(int i = 3; i < dividedLine.size(); i++){
                    rightPart.add(dividedLine.get(i));
                }
                PRODUCTION_MAP.put(Integer.parseInt(dividedLine.get(0)), new Production(dividedLine.get(1), rightPart));

            }
        }catch(IOException e){
            System.out.println(e.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

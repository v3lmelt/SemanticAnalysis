package org.example;

import org.apache.commons.math3.util.Pair;

import java.io.*;
import java.util.*;

public class GrammarAnalysis{
    private static final String BEGIN_SIGN = "s'";
    private static final String OUTPUT_FILE_NAME = "analyze.out";
    private static final String ERROR_FILE_NAME = "analyze.err";
    private static final String ANALYZER_FILE_NAME = "analyzer.xlsx";
    private static final String PRODUCTION_FILE_NAME = "production.txt";
    private File _outputFile;
    private File _errorFile;
    private FileWriter _outputFileWriter;
    private FileWriter _errorFileWriter;

    private static BufferedReader _bufferedReader;
    private static FileReader _fileReader;
    private String _textContent = "";
    private ArrayList<String> _lexeme;
    private Queue<String> _lexemeQueue;
    private Queue<String> _actionMessageQueue;
    private Queue<String> _symbolMessageQueue;
    private Queue<String> _errorMessageQueue;
    private Queue<String> _stateMessageQueue;
    private Queue<String> _lexemeMessageQueue;
    private Stack<String> _symbolStack;
    private Stack<Integer> _stateStack;
    private Integer _currentState;
    private boolean _isError;
    private Boolean _isUsingExternalFile;
    private String _externalTablePath;
    private File _externalTableFile;

    public static void getFileContent(String filePath) throws FileNotFoundException {
        _fileReader = new FileReader(filePath);
        _bufferedReader = new BufferedReader(_fileReader);
    }
    public void handleInput() throws IOException {
        String lineInput;
        while(_bufferedReader.ready()){
            lineInput = _bufferedReader.readLine();
            _textContent += lineInput;
        }
        if(_textContent != null && !_textContent.isEmpty()){
            _lexeme = new ArrayList<>(Arrays.asList(_textContent.split(" ")));
        }
        if(_lexeme != null){
            _lexeme.add("$");
            _lexemeQueue = new LinkedList<>(_lexeme);
        }
    }

    private void setOutput(String content, FileWriter fileWriter){
        if(null == fileWriter) throw new IllegalArgumentException("File writer is null!");
        else {
            System.out.print(content);
            try {
                fileWriter.write(content);
                fileWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void getOutputTable(){
        try{
            if(null != _outputFileWriter){
                _outputFileWriter.close();
            }
            _outputFileWriter = new FileWriter(_outputFile);
        } catch (IOException e){
            throw new RuntimeException(e);
        }

        setOutput(String.format("%-50s%-50s%-50s%-50s\n", "State", "Input", "Symbol", "Action"), _outputFileWriter);
//        System.out.printf("%-25s%-25s%-25s%-25s\n", "State", "Input", "Symbol", "Action");

        int index = Math.min(_actionMessageQueue.size(), _symbolMessageQueue.size());
        for(int i = 0; i < index; i++){

            setOutput(String.format("%-50s%-50s%-50s%-50s\n", _stateMessageQueue.peek(), _lexemeMessageQueue.peek(), _symbolMessageQueue.peek(), _actionMessageQueue.peek()),
                    _outputFileWriter);
//            System.out.printf("%-25s%-25s%-25s%-25s\n", _stateMessageQueue.peek(), _lexemeMessageQueue.peek(), _symbolMessageQueue.peek(), _actionMessageQueue.peek());

            _actionMessageQueue.poll();
            _symbolMessageQueue.poll();
            _stateMessageQueue.poll();
            _lexemeMessageQueue.poll();
        }
        if(!_symbolMessageQueue.isEmpty()) setOutput(_symbolMessageQueue.peek(), _outputFileWriter);
    }

    private void writeContentToFile(String content){
        try{
            File file = new File(OUTPUT_FILE_NAME);
            FileWriter writer = new FileWriter(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String dumpStackState(){
        return "Dump: \nState: " + _stateStack.toString() + " Symbol: " + _symbolStack.toString() + " Input: " + _lexemeQueue.toString();
    }
    private void setErrorMessage(String message){
        _isError = true;
        _errorMessageQueue.add(message + "\n" + dumpStackState());
        _lexemeQueue.poll();
    }

    private void getErrorMessage(){
        try{
            if(null != _errorFileWriter){
                _errorFileWriter.close();
            }
            _errorFileWriter = new FileWriter(_errorFile);
        } catch (IOException e){
            throw new RuntimeException(e);
        }

        while(!_errorMessageQueue.isEmpty()){
//            System.out.println(_errorMessageQueue.peek());
            setOutput(_errorMessageQueue.peek(), _errorFileWriter);
            _errorMessageQueue.poll();
        }
    }

    private void initStackAndQueue(){
        // 初始化堆栈状态
        _stateStack = new Stack<>();
        _symbolStack = new Stack<>();
        _actionMessageQueue = new LinkedList<>();
        _symbolMessageQueue = new LinkedList<>();
        _errorMessageQueue = new LinkedList<>();
        _stateMessageQueue = new LinkedList<>();
        _lexemeMessageQueue = new LinkedList<>();

        _stateStack.push(0);
        _symbolStack.push("$");
    }


    private void initFileStatus(){
        if(null == _errorFile) _errorFile = new File(ERROR_FILE_NAME);
        if(null == _outputFile) _outputFile = new File(OUTPUT_FILE_NAME);
        if(_errorFile.exists()){
            Boolean hasDeleted = _errorFile.delete();
        }
        if(_outputFile.exists()){
            Boolean hasDeleted = _outputFile.delete();
        }
        try{
            boolean hasCreated = _errorFile.createNewFile();
            hasCreated = _outputFile.createNewFile();
        }catch (IOException e){
            System.out.println(e);
        }
    }
    private void initProductionConstAndTable(){
        ProductionTable.setTableFile(ANALYZER_FILE_NAME);
        try {
            ProductionTable.initSymbolMap();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ProductionConst.getProductionFromFile(PRODUCTION_FILE_NAME);
    }


    public void fAnalysisInput() {
        initFileStatus();
        initStackAndQueue();
        initProductionConstAndTable();

        // 开始分析
        while(!_lexemeQueue.isEmpty()){
            _currentState = _stateStack.peek();
            // 头部不能为文法开始符号，因为文法开始符号一旦出现在symbolStack头部即规约成功
            _symbolMessageQueue.add(_symbolStack.toString());
            _stateMessageQueue.add(_stateStack.toString());
            _lexemeMessageQueue.add(_lexemeQueue.toString());
            if(!BEGIN_SIGN.equals(_symbolStack.peek()) && !_lexemeQueue.isEmpty()) {
                fAnalysisAction(_currentState, _lexemeQueue.peek());
            }else{
                break;
            }
        }
        getOutputTable();
        if(_isError) getErrorMessage();
    }

    private void fShiftAction(int stateTransferTo){
        try{
            _symbolStack.push(_lexemeQueue.peek());
            _lexemeQueue.poll();
            _stateStack.push(stateTransferTo);

            _actionMessageQueue.add("SHIFT TO: " + stateTransferTo);
        }catch(Exception e){
            System.out.println(e);
        }
    }

    private void fReduceAction(int ruleApplyTo){
        try{
            Production p = ProductionConst.PRODUCTION_MAP.get(ruleApplyTo);
            // 获取产生式的右部
            ArrayList<String> rightPart = p.getRightPart();
            Integer rightPartLen = p.getRightPartLength();
            if(!rightPart.isEmpty() && (!"eps".equals(rightPart.get(0)))){
                for(int i = 0; i < rightPartLen; i++){
                    _symbolStack.pop();
                    _stateStack.pop();
                }
                // 将右部规约成左部
                _symbolStack.push(p.getLeftPart());
                _currentState = _stateStack.peek();
                // 基于目前状态再分析一次
                if(!_symbolStack.peek().equals(BEGIN_SIGN)) fAnalysisAction(_currentState, _symbolStack.peek());
            }else if("eps".equals(rightPart.get(0))){
                _symbolStack.push(p.getLeftPart());
                _currentState = _stateStack.peek();
                fAnalysisAction(_currentState,  _symbolStack.peek());
            }

            _actionMessageQueue.add("REDUCE ACTION: " + p);
        }catch(Exception e){
            System.out.println(e.toString());
        }
    }

    private void fGotoAction(int stateTransferTo){
        try{
            _stateStack.push(stateTransferTo);
        }catch (Exception e){
            System.out.println(e);
        }
    }

    private String get2ndElementFromQueue(Queue<String> q) throws Exception {
       if(q.size() < 2) throw new IllegalArgumentException("Queue size is not correct.");
       String ans = "";
       Queue<String> p = new LinkedList<>();
       String tmp = q.poll();
       p.offer(tmp);
       ans = q.peek();
       while(!q.isEmpty()){
           p.offer(q.peek());
           q.poll();
       }
       _lexemeQueue=p;
       return ans;
    }

    private void fAnalysisAction(Integer currentState, String inputFront){
        try {
            Pair<String, Integer> actionPair = ProductionTable.getCellValue(inputFront, currentState);
            switch (actionPair.getFirst()){
                case "Goto":
                    fGotoAction(actionPair.getSecond());
                    break;
                case "Reduce":
                    fReduceAction(actionPair.getSecond());
                    break;
                case "Shift":
                    fShiftAction(actionPair.getSecond());
                    break;
                case "Predict":
                    String predict = get2ndElementFromQueue(_lexemeQueue);
                    if (predict.equals("function")) {
                        fShiftAction(26);
                    } else {
                        fReduceAction(26);
                    }
            }
            // 如果发生空指针异常说明对应单元格为空
        }catch(NullPointerException e){
            setErrorMessage("Error!");
        }catch(IllegalArgumentException e){
            setErrorMessage("QueueSize is not correct");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

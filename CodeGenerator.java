import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class CodeGenerator {
    private SimpleNode root;
    private PrintWriter out;
    private StringBuilder builder;
    private String store,classe, extend,method;
    private ArrayList<String> globals = new ArrayList<>();
    private String[] locals = new String[999];
    private int localNum = 0,ifCounter=0,whileCounter = 0,boolOpCounter = 0,temp = 0,stack = 0;
    private Stack<Integer> ifs = new Stack<>();

    public CodeGenerator(SimpleNode root) {
        this.root = root.getChild(0);
        this.builder = new StringBuilder();
        String filename = "temporary.j";

        try {
            FileWriter fileWriter = new FileWriter(filename, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            this.out = new PrintWriter(bufferedWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Starts the code generation
    public void generate() {
        if(generateImports())
            this.builder.append("\n");
        generateHeader();
        this.builder.append("\n");
        if(generateGlobals())
            this.builder.append("\n");
        generateMethods();
        // outputs the final string builded to the file
        out.println(this.builder);
        out.close();

        // File (or directory) with old name
        File file = new File("temporary.j");
        File file2 = new File(this.classe+".j");

        if (file2.exists())
            throw new java.io.IOException("file exists");
        file.renameTo(file2);
    }

    private boolean generateImports() {
        SimpleNode imp;
        boolean has=false;
        while((imp = this.root.next()) != null && imp.getName().equals("ImportDeclaration")) {
            this.builder.append(".import " + root.next().getName() + "\n");
            //this is likely wrong but i cant find the answer
            int i;
            for(i=0;i<imp.children.length-1;i++){
                this.builder.append(imp.children[i].getName()+"/")
            }
            this.builder.append(imp.children[i].getName())
            imp.getImportStatements()
            has=true;
        }
        this.root.previous();
        return has;
    }

    private void generateHeader() {
        this.classe=this.root.next().getName;
        this.builder.append(".class public " + this.classe + "\n");

        SimpleNode extend = this.root.next();
        if(extend.getName().equals("extends")){
            this.builder.append(".super "+extend.next().getName());
            this.extend = extend.same().getName();
        } else {
            this.root.previous();
            this.builder.append(".super java/lang/Object");
        }

        this.builder.append("\n");
    }

    private boolean generateGlobals() {
        SimpleNode global;
        boolean has = false;

        while((global = this.root.next()) != null && global.getName().equals("VarDeclaration")) {
            generateGlobalDeclaration(global);
            globals.add(global.next().getName());
            has = true;
        }
        this.root.previous();
        return has;
    }

    private void generateGlobalDeclaration(SimpleNode var) {

        this.builder.append(".field public '" + var.next(2).getName() + "' ");
        if(var.previous().jjtGetNumChildren() >= 3)
            this.builder.append("[");
        this.builder.append(getType(var.same().next().getName()));
        this.builder.append("\n");
    }

    private void generateMethods(){
        generateConstructor();

        SimpleNode method;
        StringBuilder save = this.builder;

        while((method = this.root.next()) != null) {
            this.builder.append("\n");
            method = method.next();
            generateFunctionHeader(method);
            generateFunctionBody(method);
            generateFunctionFooter(method);
            ifs.empty();

            StringBuilder buffer = this.builder;
            this.builder = save;
            this.builder.append(".limit stack " + this.stack + "\n.limit locals " + (this.localNum+1)+"\n" + buffer.toString()+"\n");
        }
    }

    private void generateConstructor(){
        this.builder.append(".method public <init>()V\n.limit stack 1\n.limit locals 1\n.var 0 is 'this' L" + this.classe +
                ";\n\taload_0\n\tinvokespecial ");
        if(this.extend == null)
            this.builder.append("java/lang/Object");
        else
            this.builder.append(this.extend);
        this.builder.append("/<init>()V\n\treturn\n.end method\n");
    }

    private void generateFunctionHeader(SimpleNode func){
        if(func.getName().equals("MainMethod"))
            generateMainHeader(func);
        else
            generateMethodHeader(func);
        this.builder.append("\n");

        this.builder = new StringBuilder();

        Arrays.fill(this.locals, null);
        this.localNum = 0;
        this.temp = 0;
        this.stack = 0;

        if(func.getName().equals("mainDeclaration")) {
            this.builder.append(".var 0 is args [Ljava/lang/String;\n");
        }else{
            this.builder.append(".var 0 is 'this' L" + this.classe+";\n");

            SimpleNode node;
            func.next();
            while((node = func.next()) != null && node.getName().equals("parameterDeclaration")) {
                node.reset();
                this.builder.append(".var " + (this.localNum+1) + " is ");
                String id = node.next(2).getName();
                this.builder.append("'" + id + "' ");
                node = node.previous();
                node.reset();
                if(node.jjtGetNumChildren() >= 3)
                    this.builder.append("[");
                this.builder.append(getType(node.next().getName())+"\n");
                this.locals[this.localNum] = id;
                this.localNum++;
            }
            func.reset();
            /*
                public void reset() {
                    this.idx = -1;
                }
             */
        }
    }

    private void generateMainHeader(SimpleNode func){
        this.builder.append(".method public static main([Ljava/lang/String;)V");
        this.method = "main";
        func.next(3);
        /*
    public SimpleNode next(int times) {
        SimpleNode n = null;

        if(children == null)
            return null;

        for(int i=0; i<times; i++)
            n = next();

        return n;
    }*/
    }

    private void generateMethodHeader(SimpleNode func){
        this.builder.append(".method public ");

        this.method = func.next(2).getName() + "(";
        for (int i = 0; i < func.children.length; i++) {
            if (func.children[i] instanceof ASTparameterDeclaration) {
                this.method += ((ASTparameterDeclaration) func.children[i]).getType();
            }
        }
        this.method += ")";

        this.builder.append(func.same().getName());

        SimpleNode arg;
        this.builder.append("(");
        while((arg = func.next()) != null && arg.getName().equals("ParameterDeclaration")){
            if(arg.next().jjtGetNumChildren() >= 3)
                this.builder.append("[");
            this.builder.append(getType(arg.same().next().getName()));
        }
        func.reset();
        this.builder.append(")");

        if(func.next().jjtGetNumChildren() >= 3)
            this.builder.append("[");
        this.store = func.same().next().getName();
        this.builder.append(getType(this.store));

        if(func.same().jjtGetNumChildren() >= 3)
            this.store = "int[]";
    }

    private void generateFunctionBody(SimpleNode func){
        SimpleNode body = null;
        do {
            body = func.next();
        } while(!body.getName().equals("MethodBody"));

        SimpleNode node;
        while((node = body.next()) != null && node.getName().equals("VarDeclaration") + ".var " + (this.localNum+1) + " is '" +
                node.next(2).getName() + "' ") {
            if(node.previous().jjtGetNumChildren() >= 3)
                this.builder.append("[");
            this.builder.append(getType(node.same().next().getName()));
            this.builder.append("\n");
            this.locals[this.localNum] = node.next().getName();
            this.localNum++;
        }
        body.previous();
        while((node = body.next()) != null) {
            handle(node);
        }
    }

    private void handle(SimpleNode node){
        if(node.getName().matches("-?\\d+(\\.\\d+)?")) {
            constant(node);
            inc();
        }
        else {
            switch(node.getName()){
                case "=":
                    attribution(node);
                    break;
                case "+":
                    if(!isOp(node.next()) && isOp(node.next())){
                        handle(node.same());
                        handle(node.previous());
                    } else {
                        node.reset();
                        handle(node.next());
                        handle(node.next());
                    }
                    this.builder.append("\tiadd\n");
                    this.temp--;
                    break;
                case "-":
                    if(!isOp(node.next()) && isOp(node.next())){
                        handle(node.same());
                        handle(node.previous());
                        this.builder.append("\tswap\n");
                    } else {
                        node.reset();
                        handle(node.next());
                        handle(node.next());
                    }
                    this.builder.append("\tisub\n");
                    this.temp--;
                    break;
                case "*":
                    if(!isOp(node.next()) && isOp(node.next())){
                        handle(node.same());
                        handle(node.previous());
                    } else {
                        node.reset();
                        handle(node.next());
                        handle(node.next());
                    }
                    this.builder.append("\timul\n");
                    this.temp--;
                    break;
                case "/":
                    if(!isOp(node.next()) && isOp(node.next())){
                        handle(node.same());
                        handle(node.previous());
                    } else {
                        node.reset();
                        handle(node.next());
                        handle(node.next());
                    }
                    this.builder.append("\tidiv\n");
                    this.temp--;
                    break;
                case "true":
                    this.builder.append("\ticonst_1\n");
                    inc();
                    break;
                case "false":
                    this.builder.append("\ticonst_0\n");
                    inc();
                    break;
                case "new":
                    newOperator(node);
                    break;
                case ".":
                    dotOperator(node);
                    break;
                case "array":
                    handle(node.next());
                    handle(node.next());
                    this.builder.append("\t");
                    this.builder.append("\tiaload\n");
                    this.builder.append("\n");
                    this.temp--;
                    break;
                case "<": case "&&":
                    getCondition(node, "boolOp"+boolOpCounter, false);
                    boolOp();
                    break;
                case "!":
                    getCondition(node, "boolOp"+boolOpCounter, true);
                    boolOp();
                    break;
                case "if":
                    int j = ifCounter;
                    ifs.push(new Integer(ifCounter));
                    ifCounter++;
                    handle(node.next());
                    handle(node.next());
                    this.builder.append("\tgoto endif" + j + "\n");
                    break;
                case "while":
                    this.builder.append("\twhile" + whileCounter + ":\n");
                    handle(node.next());
                    handle(node.next());
                    this.builder.append("\tgoto while" + whileCounter + "\n\tendwhile" + whileCounter + ":\n");
                    whileCounter++;
                    break;
                case "condition":
                    if (((SimpleNode) node.parent).index == JmmParserConstants.IF) {
                        getCondition(node.next(), "else"+ifs.peek(), false);
                    }
                    else {
                        getCondition(node.next(), "endwhile" + whileCounter, false);
                    }
                    break;
                case "else":
                    int jump = ifs.pop();
                    this.builder.append("\telse" + jump + ":\n");
                    handle(node.next());
                    this.builder.append("\tendif" + jump + ":\n");
                    break;
                case "body":
                    SimpleNode child;
                    while ((child = node.next()) != null) {
                        handle(child);
                    }
                    break;
                default:
                    identifier(node);
                    inc();
                    break;
            }
        }
    }

    private int find(SimpleNode node){
        for(int i=1; i <= this.localNum; i++){
            if(this.locals[i-1].equals(node.getName())){
                return i;
            }
        }
        if(node.getName().equals("this"))
            return 0;
        return 404;
    }

    private boolean isOp(SimpleNode node){
        String name = node.getName();
        return name.equals("+") || name.equals("-") || name.equals("*") || name.equals("/") ||
                name.equals("<") || name.equals("&&") || name.equals("!");
    }

    private void identifier(SimpleNode node){
        if(globals.contains(node.getName())){
            globalLoad(node);
        } else {
            this.builder.append("\t");

            if(node.getName().equals("this")){
                this.builder.append("a");
            } else {
                this.builder.append(getType2(JmmParser.getInstance().getMethod(this.method).getSymbolType(node.getName())));
            }

            int i = find(node);
            if(i > 3)
                this.builder.append("load ");
            else
                this.builder.append("load_");
            this.builder.append("" + i);
        }
        this.builder.append("\n");
    }

    private void constant(SimpleNode node){
        this.builder.append("\t");
        if(Integer.parseInt(node.getName()) > 32767)
            this.builder.append("ldc ");
        else {
            if(Integer.parseInt(node.getName()) > 127)
                this.builder.append("sipush ");
            else{
                if(Integer.parseInt(node.getName()) > 5)
                    this.builder.append("bipush ");
                else
                    this.builder.append("iconst_");
            }
        }
        this.builder.append(node.getName());
        this.builder.append("\n");
    }

    private void newOperator(SimpleNode node){
        if(node.next().getName().equals("array")){
            handle(node.same().next(2));
            this.builder.append("\tnewarray int\n");
        } else {
            this.builder.append("\tnew ");
            this.builder.append(node.same().getName());
            inc();
            this.builder.append("\n\tdup");
            inc();
            this.builder.append("\n\tinvokespecial "+node.same().getName()+"/<init>()V\n");
            this.temp--;
        }
    }

    private void attribution(SimpleNode node){
        if(!node.next().getName().equals("array")){
            int idx = find(node.same());
            if(globals.contains(node.same().getName())){
                globalStore(node);
            } else {
                handle(node.next());
                this.builder.append("\t"+getType2(JmmParser.getInstance().getMethod(this.method).getSymbolType(node.previous().getName())));
                if(idx > 3)
                    this.builder.append("store ");
                else
                    this.builder.append("store_");
                this.builder.append("" + idx);
                this.temp--;
            }
        } else {
            handle(node.same().next());
            handle(node.same().next());
            handle(node.next());
            this.builder.append("\tiastore");
            this.temp -= 3;
        }
        this.builder.append("\n");
    }

    private void globalStore(SimpleNode node){
        this.builder.append("\taload_0");
        inc();
        this.builder.append("\n");
        handle(node.next());
        this.builder.append("\tputfield ");
        this.temp -= 2;
        this.builder.append(this.classe+"/"+node.previous().getName()+" "+
                getType(JmmParser.getInstance().getMethod(this.method).getSymbolType(node.same().getName())));
    }

    private void globalLoad(SimpleNode node){
        this.builder.append("\taload_0\n\tgetfield"+this.classe+"/"+node.getName()+" "+
                getType(JmmParser.getInstance().getMethod(this.method).getSymbolType(node.getName())));
    }

    private void dotOperator(SimpleNode node) {
        if (node.next(2).index == JmmParserConstants.LENGTH) {
            handle(node.previous());
            this.builder.append("\tarraylength\n");
            return;
        }

        functionCall(node.previous(), node.next());
    }

    private void functionCall(SimpleNode caller, SimpleNode call) {
        SimpleNode parameters = call.next(2);
        SimpleNode param;
        String callerId = "" + find(caller);

        if(caller.getName().equals("new")){
            newOperator(caller);
            caller.reset();
            caller = caller.next();
            callerId = "new";
        } else {
            if (!callerId.equals("404"))
                handle(caller);
        }

        if (parameters != null){
            while((param = parameters.next()) != null)
                handle(param);
            parameters.reset();
        }

        this.builder.append("\t");
        switch(callerId){
            case "404":
                this.builder.append("invokestatic " + caller.getName() + "/" + call.previous().getName() + "(");
                break;
            case "new":
                this.builder.append("invokevirtual " + caller.getName() + "/" + call.previous().getName() + "(");
                this.temp--;
                break;
            default:
                this.builder.append("invokevirtual ");
                if(caller.getName().equals("this"))
                    this.builder.append(this.classe);
                else
                    this.builder.append(JmmParser.getInstance().getMethod(this.method).getSymbolType(caller.getName())
                    +"/" + call.previous().getName() + "(");
                this.temp--;
                break;
        }

        SymbolTable method;
        if (parameters != null) {
            while((param = parameters.next()) != null) {
                String name = param.getName();
                if(name.equals("array")){
                    param.reset();
                    this.builder.append("I");
                    continue;
                }
                if (name.matches("-?\\d+(\\.\\d+)?") || isOp(param))
                    this.builder.append("I");
                else if (name.equals("true") || name.equals("false"))
                    this.builder.append("Z");
                else if (!(""+find(param)).equals("404") || globals.contains(name))
                    this.builder.append(getType(JmmParser.getInstance().getMethod(this.method).getSymbolType(name)));
                else if (name.equals(".")){
                    param.reset();
                    param.next(2).reset();
                    if(param.same().getName().equals("length")){
                        this.builder.append("I");
                    } else {
                        method = JmmParser.getInstance().getMethod(((ASTfunctionCall) param.same()).getMethodName());
                        returns(method, param.same(), false);
                    }
                }
            }
            this.temp -=parameters.children.length;
        }

        this.builder.append(")");
        method = JmmParser.getInstance().getMethod(((ASTfunctionCall) call).getMethodName());
        returns(method, call, true);
        this.builder.append("\n");
    }

    private void returns(SymbolTable method, SimpleNode call, boolean pop){
        String parentName = ((SimpleNode) call.jjtGetParent().jjtGetParent()).getName();
        String type;
        if (method != null) {
            type = getType(method.getType());
            if(!type.equals("V"))
                inc();
            this.builder.append(type);
            if(!parentName.equals("!") && !parentName.equals("parameters") && !parentName.equals("return") && !parentName.equals("condition") && pop && !type.equals("V") && findReturnType(call).equals("void")){
                this.builder.append("\n\tpop");
                this.temp--;
            }
        } else {
            type = getType(findReturnType(call));
            if(!type.equals("V"))
                inc();
            this.builder.append(type);
        }
    }

    private String findReturnType(SimpleNode call) {
        SimpleNode parentElement = (SimpleNode) call.jjtGetParent().jjtGetParent();
        String parentName = parentElement.getName();
        if (parentName.equals("+") || parentName.equals("-") || parentName.equals("*") || parentName.equals("/") || parentName.equals("<")) {
            return "int";
        }
        else if(parentName.equals("array")){
            for(int i = 0; i < parentElement.children.length; i++){
                SimpleNode node = (SimpleNode)parentElement.children[i];
                if(node != null && node.getName().equals(".") && node.children[1] == call){
                    if(i == 0)
                        return "int[]";
                    else
                        return "int";
                }
            }
        }
        else if (parentName.equals("&&") || parentName.equals("!") || parentName.equals("condition")) {
            return "boolean";
        }
        else if (parentName.equals("=")) {
            if(((SimpleNode) parentElement.children[0]).getName().equals("array"))
                return "int";
            else
                return JmmParser.getInstance().getMethod(this.method).getSymbol(((SimpleNode) parentElement.children[0]).getName()).getType();
        }
        return "void";
    }

    private void boolOp() {
        this.builder.append("\ticonst_1\n\tgoto endBoolOp" + boolOpCounter + "\n\tboolOp" + boolOpCounter + ":\n\ticonst_0\n\tendBoolOp"+
                boolOpCounter + ":\n");
        boolOpCounter++;
        inc();
    }

    private void getCondition(SimpleNode node, String jump, boolean invert) {
        if (node.getName().equals("<")) {
            if(!isOp(node.next()) && isOp(node.next())){
                handle(node.same());
                handle(node.previous());
                this.builder.append("\tswap\n");
            } else {
                node.reset();
                handle(node.next());
                handle(node.next());
            }
            this.builder.append("\t");
            if (invert)
                this.builder.append("if_icmplt " + jump);
            else
                this.builder.append("if_icmpge " + jump);
            this.builder.append("\n");
            this.temp -= 2;
        }
        else if (node.getName().equals("&&")) {
            getCondition(node.next(), jump, invert);
            getCondition(node.next(), jump, invert);
        }
        else if (node.getName().equals("!")) {
            getCondition(node.next(), jump, !invert);
        }
        else {
            handle(node);
            this.builder.append("\t");
            if (invert)
                this.builder.append("ifne " + jump);
            else
                this.builder.append("ifeq " + jump);
            this.builder.append("\n");
            this.temp--;
        }
    }

    private void generateFunctionFooter(SimpleNode func){
        SimpleNode n = func.next();
        if(n != null){
            handle(n.next());
            this.builder.append("\t"+getType2(this.store));
            this.temp--;
        } else{
            this.builder.append("\t");
        }
        this.builder.append("return\n");

        this.builder.append(".end method\n");
    }

    private String getType(String type){
        switch(type){
            case "int":
                return "I";
            case "boolean":
                return "Z";
            case "void":
                return "V";
            case "int[]":
                return "[I";
        }

        return "L" + type + ";";
    }

    private String getType2(String type){
        switch(type){
            case "int": case "boolean":
                return "i";
        }

        return "a";
    }

    private void inc(){
        this.temp++;
        if(this.temp > this.stack)
            this.stack = this.temp;
    }

    private void add(int add){
        this.temp += add;
        if(this.temp > this.stack)
            this.stack = this.temp;
    }
}

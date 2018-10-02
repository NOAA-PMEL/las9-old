package pmel.sdig.las.shared.autobean;

public class Constraint {
    String type;
    String lhs;
    String op;
    String rhs;

    public Constraint() {
    }

    public Constraint(String type, String lhs, String op, String rhs) {
        this.type = type;
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLhs() {
        return lhs;
    }

    public void setLhs(String lhs) {
        this.lhs = lhs;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getRhs() {
        return rhs;
    }

    public void setRhs(String rhs) {
        this.rhs = rhs;
    }
}

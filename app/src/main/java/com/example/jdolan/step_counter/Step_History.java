package com.example.jdolan.step_counter;

import java.util.Date;
/**
 * Created by jdolan on 25/04/16.
 */
public class Step_History {
    private long id;
    private long Steps;
    private Date Start;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSteps() {
        return Steps;
    }

    public void setSteps(long steps) {
        Steps = steps;
    }

    public Date getStart() {
        return Start;
    }

    public void setStart(Date start)
    {
        Start = start;
    }

  /*  @Override
    public String toString() { return Step_History; } */

   //  @Override
  //   public String toString() {//
    // return Step_History;
    // }


 /*   public long getId() {
        return id;////
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getComment() {
        return Steps;
    }

    public void setComment(String comment) {
        this.Steps = Steps;
    }

    // Will be used by the ArrayAdapter in the ListView
    //@Override
   // public String toString() {
        //return Step_History;
   // } */
}

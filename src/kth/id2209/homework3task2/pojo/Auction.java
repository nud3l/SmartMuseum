package kth.id2209.homework3task2.pojo;

/**
 * Created by nud3l on 11/21/16.
 */
public class Auction {
    private int initialPrice;
    private String artworkName;
    private int reservePrice;
    private int reductionRate;

    public Auction(int initialPrice, String artworkName, int reservePrice, int reductionRate) {
        this.initialPrice = initialPrice;
        this.artworkName = artworkName;
        this.reservePrice = reservePrice;
        this.reductionRate = reductionRate;
    }

    public int getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(int initialPrice) {
        this.initialPrice = initialPrice;
    }

    public String getArtworkName() {
        return artworkName;
    }

    public void setArtworkName(String artworkName) {
        this.artworkName = artworkName;
    }

    public int getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(int reservePrice) {
        this.reservePrice = reservePrice;
    }

    public int getReductionRate() {
        return reductionRate;
    }

    public void setReductionRate(int reductionRate) {
        this.reductionRate = reductionRate;
    }
}

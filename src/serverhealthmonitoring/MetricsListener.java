/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package serverhealthmonitoring;

public interface MetricsListener {

    void onMetrics(String address, Metrics m);

    void onStatus(String address, String status);
}


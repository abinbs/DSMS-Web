package org.example;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface DSMSService {
    //Admin Operations
    @WebMethod
    String addShare(String shareId, String shareType, int capacity);
    @WebMethod
    String removeShare(String shareId, String shareType);
    @WebMethod
    String listShareAvailability(String shareType);

    //Buyer Operations
    @WebMethod
    String purchaseShare(String buyerId, String shareId, String shareType, int shareCount);
    @WebMethod
    String getShares(String buyerId);
    @WebMethod
    String sellShare(String buyerId, String shareId, int shareCount);
    @WebMethod
    String swapShares(String buyerID, String oldShareID, String oldShareType, String newShareID, String newShareType);
}

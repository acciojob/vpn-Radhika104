package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
      User user=userRepository2.findById(userId).get();
      CountryName countryName1=CountryName.valueOf(countryName.toUpperCase());
      if(user.getConnected()) throw new Exception("Already connected");
      else if(countryName1.equals(user.getOriginalCountry())) return user;

      if(user.getServiceProviderList().isEmpty()) throw new Exception("Unable to connect");

      List<ServiceProvider> serviceProviderList=user.getServiceProviderList();
      int lowestId=Integer.MIN_VALUE;
      ServiceProvider serviceProvider=null;
      for(ServiceProvider serviceProvider1:serviceProviderList)
      {
          for(Country country:serviceProvider1.getCountryList())
          {
              if(country.getCountryName().toString().equalsIgnoreCase(countryName))
              {
                  if(serviceProvider==null || lowestId>serviceProvider1.getId())
                  {
                      lowestId=serviceProvider1.getId();
                      serviceProvider=serviceProvider1;
                  }
              }
          }
      }
      if(serviceProvider==null) throw new Exception("Unable to connect");
      String maskedIp=countryName1.toCode()+"."+serviceProvider.getId()+"."+userId;
      user.setMaskedIp(maskedIp);
      user.setConnected(Boolean.TRUE);

      Connection connection=new Connection();
      connection.setUser(user);
      connection.setServiceProvider(serviceProvider);
      user.getConnectionList().add(connection);
      serviceProvider.getConnectionList().add(connection);
      userRepository2.save(user);
      serviceProviderRepository2.save(serviceProvider);
      return user;

    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user=userRepository2.findById(userId).get();
        if(!user.getConnected()) throw new Exception("Already disconnected");

        user.setMaskedIp(null);
        user.setConnected(Boolean.FALSE);
        userRepository2.save(user);
        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User receiver=userRepository2.findById(receiverId).get();
        CountryName receiverCountryName=null;
        if(receiver.getConnected()){
            String maskedCode= receiver.getMaskedIp().substring(0,3);
            if(maskedCode.equals("001"))
                receiverCountryName=CountryName.IND;
            else if(maskedCode.equals("002"))
                receiverCountryName=CountryName.USA;
            else if(maskedCode.equals("003"))
                receiverCountryName=CountryName.AUS;
            else if(maskedCode.equals("004"))
                receiverCountryName=CountryName.CHI;
            else if(maskedCode.equals("005"))
                receiverCountryName=CountryName.JPN;
        }
        else{
            receiverCountryName=receiver.getOriginalCountry().getCountryName();
        }
        User user=null;
        try{
            user=connect(senderId,receiverCountryName.toString());
        }
        catch(Exception e){
            throw new Exception("Cannot establish communication");
        }
        return user;

    }
}

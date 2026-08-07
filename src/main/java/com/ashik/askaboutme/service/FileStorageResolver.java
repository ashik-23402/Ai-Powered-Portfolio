package com.ashik.askaboutme.service;

import com.ashik.askaboutme.configdto.StorageProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FileStorageResolver {

    private final Map<StorageProvider, FileStorageService> storageServices;


    public FileStorageResolver(List<FileStorageService> services) {

        this.storageServices =
                services.stream()
                        .collect(Collectors.toMap(
                                FileStorageService::provider,
                                Function.identity()
                        ));
    }


    public FileStorageService resolve(StorageProvider provider) {

        FileStorageService service =
                storageServices.get(provider);


        if (service == null) {
            throw new IllegalArgumentException(
                    "Unsupported storage provider: " + provider
            );
        }


        return service;
    }
}

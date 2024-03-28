package io.nanasintown.contactapi.service;

import io.nanasintown.contactapi.domain.Contact;
import io.nanasintown.contactapi.repo.ContactRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.nanasintown.contactapi.constant.Constant.PHOTO_DIR;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ContactService {
    private final ContactRepo contactRepo;

//    Return a page of contacts
    public Page<Contact> getAllContact(int page, int size){
        return contactRepo.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    public Contact getContact(String id){
        return contactRepo.findById(id).orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    public Contact createContact(Contact contact){
        return contactRepo.save(contact);
    }

    public void deleteContact(Contact contact){
        contactRepo.delete(contact);
    }

    public String uploadPhoto(String id, MultipartFile file){
        Contact contact = getContact(id);
        String photoUrl = photoFunc.apply(id, file);
        contact.setPhotoUrl(photoUrl);
        contactRepo.save(contact);
        return photoUrl;
    }

    private final Function<String, String> fileExtension = fileName ->
            Optional.of(fileName)
            .filter(name -> name.contains("."))
            .map(name -> "." + name.substring(fileName.lastIndexOf(".") +1)).orElse(".png");


    private final BiFunction<String, MultipartFile, String> photoFunc = (id, image) -> {
        String extension = id + fileExtension.apply(image.getOriginalFilename());
        try{
            Path fileLocation = Paths.get(PHOTO_DIR).toAbsolutePath().normalize();
            if(!Files.exists(fileLocation)){
                Files.createDirectories(fileLocation);
            }
            Files.copy(image.getInputStream(), fileLocation.resolve(extension), REPLACE_EXISTING);
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/contacts/image/" + extension).toUriString();
        } catch (Exception exc){
            throw new RuntimeException("Unable to save image");

        }
    };
}


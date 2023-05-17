package jpabook.jpashop.Controller;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping(value = "/items/new")
    public String createForm(Model model) {
        model.addAttribute("form", new BookForm());
        return "items/createItemForm";
    }

    @PostMapping(value = "/items/new")
    public String create(BookForm form) {

        Book book = new Book();
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book);
        return "redirect:/items";
    }

    @GetMapping("/items")
    public String list(Model model) {
        List<Item> items = itemService.findItems();
        model.addAttribute("items", items);
        return "items/itemList";
    }

    @GetMapping("/items/{itemId}/edit")
    public String updateItemForm(@PathVariable("itemId") Long itemId, Model model) {

        Book item = (Book) itemService.findOne(itemId);

        BookForm form = new BookForm();
        form.setId(item.getId());
        form.setName(item.getName());
        form.setPrice(item.getPrice());
        form.setStockQuantity(item.getStockQuantity());
        form.setAuthor(item.getAuthor());
        form.setIsbn(item.getIsbn());

        model.addAttribute("form", form);
        return "items/updateItemForm";
    }

    @PostMapping("/items/{itemId}/edit")
    public String updateItem(@PathVariable("itemId") Long itemId, @ModelAttribute("form") BookForm form) {

        /** 준영속 엔티티
         * => 영속성 컨텍스트가 더이상 관리하지 않는 엔티티
         * (한번 db에 저장되었다가 나온? 원래 있는데 new로 생성해서? => update해야하는지 모름)
         *
         * => 현재 book 엔티티 인스턴스는 준영속 상태이므로
         *    영속성 컨텍스트의 지원을 받을 수 없고 데이터를 수정해도 변경 감지 기능은 동작하지 않음
         *
         * ====> 준영속 엔티티를 수정하는 방법 <=================================================
         * 1. 변경 감지 기능 사용
         *    @Transactional
         *    void update (Item itemParam) { // itemParam : 파라미터로 넘어온 준영속 상태의 엔티티
         *        Item findItem = em.find(Item.class, itemParam.getId()); // 같은 엔티티 조회
         *        findItem.setPrice(itemParam.getPrice()); // 데이터 수정
         *    }
         *    => 같은 트랜잭션 안에서 엔티티 다시 조회, 변경할 값 선택
         *       => 트랜잭션 커밋 시점에 변경 감지(Dirty Checking)이 동작 (update sql 실행)
         *
         * 2. 병합(merge) 사용
         *      : 준영속 상태의 엔티티를 영속 상태로 변경할 때 사용하는 기능.
         *    @Transactional
         *    void update (Item itemParam) { // itemParam : 파라미터로 넘어온 준영속 상태의 엔티티
         *        Item mergeItem = em.merge(itemParam);
         *    }
         *    ====== 병합 동작 방식 ========
         *    1. 준영속 엔티티의 식별자 값으로 영속 엔티티를 조회한다.
         *    2. 영속 엔티티의 값을 준영속 엔티티의 값으로 모두 교체한다.(병합한다.)
         *    3. 트랜잭션 커밋 시점에 변경 감지 기능이 동작해서 데이터베이스에 UPDATE SQL이 실행
         *
         * ====== 주의 할 점 ======================
         * 변경 감지 기능을 사용하면 원하는 속성만 선택해서 변경할 수 있지만, 병합을 사용하면 모든 속성이 변경된다.
         * 병합시 값이 없으면 null 로 업데이트 할 위험도 있다. (병합은 모든 필드를 교체한다.)
         * => 변경 감지 사용 권장
         */
        // 병합 방식
        // Book book = new Book();
        // book.setId(form.getId());
        // book.setName(form.getName());
        // book.setPrice(form.getPrice());
        // book.setStockQuantity(form.getStockQuantity());
        // book.setAuthor(form.getAuthor());
        // book.setIsbn(form.getIsbn());
        // itemService.saveItem(book);

        itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());
        return "redirect:/items";
    }
}

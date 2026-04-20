# Tài liệu Frontend - CATEGORY (Quản Lý Danh Mục)

Tài liệu này trình bày chi tiết về cấu trúc mã nguồn, API services và UI components bám sát chuẩn cho tính năng Quản lý danh mục trong ứng dụng frontend React/Vite.

---

## 📌 Cấu trúc trả về chuẩn và payload JSON (API Mappings)

Toàn bộ các thao tác gọi API đối với `/v1/categories` đều được cấu hình tập trung tại file `src/services/categoryService.js`. Kết quả trả về (response) được ép theo định dạng chuẩn của server thông qua axios interceptors.

### 🟢 Lấy danh sách danh mục (GET `/v1/categories`)
**Mô tả:** Lấy danh sách chuyên mục (Expense/Income), bao gồm của user đang đăng nhập và các danh mục mặc định của hệ thống.
- **Query Params:**
  - `type` (optional): `expense` hoặc `income`
  - `include_inactive` (optional): `true` (nếu muốn lấy cả các danh mục đã bị ẩn/tắt - dùng trong màn hình Quản lý danh mục. Mặc định là `false` - chỉ lấy danh mục đang hoạt động, dùng trong màn hình tạo Giao dịch).

**Response Example (200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách thành công",
  "data": [
    {
      "id": "e2ba34dc-8a21-4f9a-9d21-f123456789ab",
      "user_id": "c393bcaf-...",    // null nếu là danh mục mặc định của hệ thống
      "name": "Ăn uống",
      "type": "expense",
      "icon_name": "ic_food",
      "is_active": true
    },
    {
      "id": "a9cb14df-91bd-421c-8fbd-d987654321cd",
      "user_id": null,
      "name": "Lương",
      "type": "income",
      "icon_name": "ic_salary",
      "is_active": true
    }
  ]
}
```

### 🟢 Tạo mới danh mục (POST `/v1/categories`)
**Mô tả:** Dùng khi người dùng bấm "Thêm danh mục". `icon_name` tương ứng với ID của icon hiển thị trong `assets/icons/`.

**Payload Request:**
```json
{
  "name": "Thú cưng",
  "type": "expense",
  "icon_name": "ic_other"
}
```
**Response Example (201 Created):**
```json
{
  "success": true,
  "message": "Thêm danh mục mới thành công",
  "data": {
    "id": "b3cd45ab-1234-abcd-efgh-123456789012",
    "user_id": "c393bcaf-...",
    "name": "Thú cưng",
    "type": "expense",
    "icon_name": "ic_other",
    "is_active": true
  }
}
```
**Response Error (400 Bad Request / Validation Error):**
```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "errors": [
     { "field": "name", "message": "Tên danh mục không được để trống" }
  ]
}
```

### 🟢 Cập nhật danh mục (PATCH `/v1/categories/:id`)
**Mô tả:** Chỉ có thể cập nhật các danh mục thuộc quyền sở hữu của user (`user_id !== null`).

**Payload Request:**
```json
{
  "name": "Ăn uống ở nhà",
  "icon_name": "ic_food",
  "is_active": false
}
```
**Response Example (200 OK):**
```json
{
  "success": true,
  "message": "Cập nhật thành công",
  "data": {
    "id": "e2ba34dc-8a21-4f9a-9d21-f123456789ab",
    "name": "Ăn uống ở nhà",
    "type": "expense",
    "icon_name": "ic_food",
    "user_id": "c393bcaf-...",
    "is_active": false
  }
}
```

### 🟢 Xóa danh mục (DELETE `/v1/categories/:id`)
**Mô tả:** Cho phép client loại bỏ danh mục mềm hoặc xoá vĩnh viễn (phụ thuộc vào tuỳ chỉnh DB backend). Tương tự không thể xóa danh mục hệ thống.

**Response Example (200 OK):**
```json
{
  "success": true,
  "message": "Xóa danh mục thành công" // Xóa hoàn toàn
  // "data": null
}
```

---

## 🏗 Tổ chức Component (Cấu trúc Thư mục Frontend)

UI được chia module hóa, tuân thủ nguyên tắc Smart/Dumb Component và custom hooks.

### 1. `src/services/categoryService.js`
Định nghĩa Service Layer chứa các phương thức tương tác API. Hàm trả về cấu trúc đã `.data` thông qua Axios (ví dụ `return await apiClient.get('/categories')`).
- Cung cấp: `getCategories(type, includeInactive)` với flag `includeInactive` để quản lý việc gọi API cho hai mục đích: màn hình quản lý (lấy tất cả) và màn hình giao dịch (loại bỏ chuyên mục đã ẩn).

### 2. `src/hooks/useCategories.js`
Custom Hook chứa Application Logic và State:
- **`categories`**: Mảng lưu trữ danh sách đang hiển thị.
- **`loading`**: Boolean xử lý UX trạng thái loading indicator.
- **`currentType` (useRef)**: Lưu biến `filterType` để đảm bảo list được fetch lại đúng category type khi Thêm/Sửa/Xóa.
- **`fetchCategories(type, includeInactive)`**, **`addCategory()`**, **`editCategory()`**, **`removeCategory()`**: Các action methods đồng bộ dữ liệu và hiển thị toast pop-up status bằng `useAppMessage()`. Bổ sung action update `is_active` để ẩn/hiện category.

### 3. `src/pages/categories/Categories.jsx`
Page Component chính (Controller):
- Hiển thị Toolbar gòm thanh Select Box (lọc `expense`/`income`) và Button "Thêm danh mục". Tích hợp gọi `fetchCategories(type, true)` để hiển thị mọi trạng thái.
- Cung cấp state để quản lý việc bật mở Modal.
- Rendering danh sách dựa vào Component con `CategoryCard`.
- Pass props callback (`onEdit`, `onDelete`, hàm `handleToggleActive` chung).

### 4. `src/pages/categories/components/CategoryCard.jsx`
Presentation Component:
- Nhận prop `category`. 
- Fetch icon file trực tiếp từ `/src/assets/icons/{icon_name}.svg`. Map biến `type` thành "Chi tiêu" hoặc "Thu nhập".
- Bổ sung **Toggle Switch** on/off cho mọi category (hệ thống/user mới). Khi tắt (inactive), thẻ sẽ tự động áp dụng CSS `opacity-70`, ảnh `grayscale` và gạch ngang chữ `line-through`.
- Kiểm tra điều kiện: hiển thị Nhãn "Hệ thống" xanh (nếu `user_id === null`) đồng thời khoá (ẩn đi) 2 nút Sửa và Xóa. Người dùng vẫn được quyền Turn On/Off các Nhóm hệ thống thông qua Toggle.

### 5. `src/pages/transactions/Transactions.jsx` (Impacted Component)
- Update cơ chế thêm sửa Giao dịch: Truyền flag `includeInactive = false` vào service để khi hiện danh sách Select Box, các chuyên mục bị Off/Inactive ở hệ thống sẽ tự động biến mất, tránh tạo mới chi phí sai lệch.

### 6. `src/pages/categories/CategoryModal.jsx`
Reusable Component cho form hành động (Thêm/Sửa):
- Quản lý Local State `formData` bằng `useState`. Cập nhật tự động nếu prop `category` tồn tại (thông qua `useEffect` - Edit Mode).
- **Lưới Icon Picker:** Render mảng hằng số `ICON_LIST` các icon `.svg`. Khi nhấp vào item, gán biến đó vào `icon_name`.
- Trả về Promise đợi server hoàn tất quá trình cập nhật trước khi đổi nút Lưu ("Lưu lại" => Loading Spinner).

---

## 🔗 Cấu hình Router & Navigation UI

* **App Routes (`src/App.jsx`)**: Import Lazy page `Categories` và thêm `<Route path="categories" element={<Categories />} />` nằm trong `ProtectedRoute`.
* **Sidebar Layout (`src/layouts/Sidebar.jsx`)**: Đăng ký một NavLink trỏ link sang `/categories` đi kèm thẻ biểu tượng Tag/Phân loại. Cập nhật class active khi đường dẫn URL hiện tại là `/categories`.

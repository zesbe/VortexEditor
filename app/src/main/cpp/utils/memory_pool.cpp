#include "memory_pool.h"

namespace videoeditor {

MemoryPool::MemoryPool(size_t blockSize, size_t numBlocks)
    : m_blockSize(blockSize)
    , m_numBlocks(numBlocks) {
    m_pool.resize(blockSize * numBlocks);
    m_freeBlocks.reserve(numBlocks);
    
    for (size_t i = 0; i < numBlocks; i++) {
        m_freeBlocks.push_back(m_pool.data() + i * blockSize);
    }
}

MemoryPool::~MemoryPool() = default;

void* MemoryPool::allocate() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_freeBlocks.empty()) {
        return nullptr;
    }
    
    void* block = m_freeBlocks.back();
    m_freeBlocks.pop_back();
    return block;
}

void MemoryPool::deallocate(void* ptr) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    // Verify ptr is within our pool
    uint8_t* ptrByte = static_cast<uint8_t*>(ptr);
    if (ptrByte >= m_pool.data() && ptrByte < m_pool.data() + m_pool.size()) {
        m_freeBlocks.push_back(ptr);
    }
}

void MemoryPool::reset() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    m_freeBlocks.clear();
    for (size_t i = 0; i < m_numBlocks; i++) {
        m_freeBlocks.push_back(m_pool.data() + i * m_blockSize);
    }
}

}  // namespace videoeditor
